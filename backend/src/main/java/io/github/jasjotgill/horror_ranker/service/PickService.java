package io.github.jasjotgill.horror_ranker.service;

import java.time.Year;
import java.util.Objects;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.domain.Pick;
import io.github.jasjotgill.horror_ranker.domain.SeenFlag;
import io.github.jasjotgill.horror_ranker.dto.MovieSearchResult;
import io.github.jasjotgill.horror_ranker.dto.PickRequest;
import io.github.jasjotgill.horror_ranker.dto.PickResponse;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import io.github.jasjotgill.horror_ranker.repository.PickRepository;
import io.github.jasjotgill.horror_ranker.repository.SeenFlagRepository;
import io.github.jasjotgill.horror_ranker.tmdb.TmdbClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickService {

	// A pick is vetoed once this many members have seen it.
	static final int SEEN_TO_VETO = 2;

	private static final int MAX_TITLE_LENGTH = 200;

	private static final int MAX_POSTER_URL_LENGTH = 500;

	// The year of the first film ever made.
	private static final int EARLIEST_YEAR = 1888;

	private final MovieGroupRepository groups;

	private final PickRepository picks;

	private final SeenFlagRepository seenFlags;

	private final TmdbClient tmdb;

	public PickService(MovieGroupRepository groups, PickRepository picks, SeenFlagRepository seenFlags,
			TmdbClient tmdb) {
		this.groups = groups;
		this.picks = picks;
		this.seenFlags = seenFlags;
		this.tmdb = tmdb;
	}

	@Transactional
	public PickResponse submitPick(Member member, PickRequest request) {
		// Re-read the group inside this transaction so the status check sees current data.
		MovieGroup group = groups.findById(member.getGroup().getId())
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() != GroupStatus.LOBBY) {
			throw ApiException.conflict("Picks are locked once the marathon has started");
		}
		Pick candidate = buildPick(group, member, request);

		Pick ownLivePick = null;
		for (Pick existing : picks.findByGroupId(group.getId())) {
			boolean ownLive = !existing.isVetoed() && existing.getMember().getId().equals(member.getId());
			if (ownLive) {
				ownLivePick = existing;
			}
			else if (sameFilm(existing, candidate)) {
				// Same wording whoever picked it, so the reply says nothing about the picker.
				throw ApiException.conflict(existing.isVetoed() ? "Too many people here have seen that film. Choose another."
						: "That film is already picked");
			}
		}
		if (ownLivePick != null) {
			picks.delete(ownLivePick);
			// Hibernate would otherwise run the INSERT before the DELETE and trip the
			// one-live-pick-per-member index.
			picks.flush();
		}
		return PickResponse.from(picks.save(candidate), 0);
	}

	// "I've seen it". Each member counts once per pick. When SEEN_TO_VETO members have
	// seen a film, the pick is vetoed and its picker has to choose again.
	@Transactional
	public void markSeen(Member member, Long pickId) {
		Pick pick = findLobbyPick(member, pickId);
		if (pick.getMember().getId().equals(member.getId())) {
			throw ApiException.conflict("You cannot flag your own pick. Change it instead.");
		}
		if (seenFlags.findByPickIdAndMemberId(pickId, member.getId()).isEmpty()) {
			// saveAndFlush so the count below includes this flag.
			seenFlags.saveAndFlush(new SeenFlag(pick, member));
		}
		if (seenFlags.countByPickId(pickId) >= SEEN_TO_VETO) {
			// No save call needed: the entity is managed, so the change is written at commit.
			pick.setVetoed(true);
		}
	}

	// Undo for a mistaken tap. Has no effect once the pick has been vetoed.
	@Transactional
	public void unmarkSeen(Member member, Long pickId) {
		findLobbyPick(member, pickId);
		seenFlags.findByPickIdAndMemberId(pickId, member.getId()).ifPresent(seenFlags::delete);
	}

	private Pick findLobbyPick(Member member, Long pickId) {
		Pick pick = picks.findById(pickId)
			.filter(found -> !found.isVetoed() && found.getGroup().getId().equals(member.getGroup().getId()))
			// A pick in someone else's group looks exactly like one that does not exist.
			.orElseThrow(() -> ApiException.notFound("No such pick"));
		// Locked so flags arriving together are counted one after another, not all as "one short".
		MovieGroup group = groups.findWithLockById(pick.getGroup().getId())
			.orElseThrow(() -> ApiException.notFound("No such pick"));
		if (group.getStatus() != GroupStatus.LOBBY) {
			throw ApiException.conflict("Films can only be flagged before the marathon starts");
		}
		return pick;
	}

	private Pick buildPick(MovieGroup group, Member member, PickRequest request) {
		boolean hasTmdbId = request.tmdbId() != null;
		boolean hasManualFields = request.title() != null || request.year() != null || request.posterUrl() != null;
		if (hasTmdbId == hasManualFields) {
			throw ApiException.badRequest("Send either tmdbId, or title and year, but not both");
		}
		return hasTmdbId ? buildTmdbPick(group, member, request.tmdbId()) : buildManualPick(group, member, request);
	}

	// Title, year and poster come from TMDB, not from the client.
	private Pick buildTmdbPick(MovieGroup group, Member member, long tmdbId) {
		MovieSearchResult movie = tmdb.findById(tmdbId)
			.orElseThrow(() -> ApiException.badRequest("No film with that TMDB id"));
		if (movie.year() == null) {
			throw ApiException.badRequest("That film has no release year on TMDB. Add it manually instead.");
		}
		return Pick.fromTmdb(group, member, tmdbId, movie.title(), movie.year(), movie.posterUrl());
	}

	private Pick buildManualPick(MovieGroup group, Member member, PickRequest request) {
		String title = request.title() == null ? "" : request.title().strip();
		if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
			throw ApiException.badRequest("title must be between 1 and " + MAX_TITLE_LENGTH + " characters");
		}
		int latestYear = Year.now().getValue() + 1;
		if (request.year() == null || request.year() < EARLIEST_YEAR || request.year() > latestYear) {
			throw ApiException.badRequest("year must be between " + EARLIEST_YEAR + " and " + latestYear);
		}
		return Pick.manual(group, member, title, request.year(), cleanPosterUrl(request.posterUrl()));
	}

	private static String cleanPosterUrl(String posterUrl) {
		if (posterUrl == null || posterUrl.isBlank()) {
			return null;
		}
		String url = posterUrl.strip();
		if (!url.startsWith("https://") || url.length() > MAX_POSTER_URL_LENGTH) {
			throw ApiException.badRequest(
					"posterUrl must start with https:// and be at most " + MAX_POSTER_URL_LENGTH + " characters");
		}
		return url;
	}

	// The same TMDB id, or the same normalized title and year. The second rule is what
	// catches a film entered once through search and once by hand.
	private static boolean sameFilm(Pick a, Pick b) {
		if (a.getTmdbId() != null && Objects.equals(a.getTmdbId(), b.getTmdbId())) {
			return true;
		}
		return a.getReleaseYear().equals(b.getReleaseYear())
				&& TitleNormalizer.normalize(a.getTitle()).equals(TitleNormalizer.normalize(b.getTitle()));
	}

}
