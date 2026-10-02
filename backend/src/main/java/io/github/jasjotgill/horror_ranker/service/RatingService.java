package io.github.jasjotgill.horror_ranker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.domain.Pick;
import io.github.jasjotgill.horror_ranker.domain.Rating;
import io.github.jasjotgill.horror_ranker.dto.FilmResult;
import io.github.jasjotgill.horror_ranker.dto.GoldenTicketResult;
import io.github.jasjotgill.horror_ranker.dto.RatingRequest;
import io.github.jasjotgill.horror_ranker.dto.RatingResponse;
import io.github.jasjotgill.horror_ranker.dto.ResultsResponse;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.GoldenTicketRepository;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import io.github.jasjotgill.horror_ranker.repository.PickRepository;
import io.github.jasjotgill.horror_ranker.repository.RatingRepository;
import io.github.jasjotgill.horror_ranker.service.ScoreCalculator.FilmScore;
import io.github.jasjotgill.horror_ranker.service.ScoreCalculator.Scores;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

	// boost is what the golden ticket added to this film's score.
	private record ScoredPick(Pick pick, FilmScore score, double boost) {
	}

	private static final Comparator<ScoredPick> RANKING = Comparator
		.comparing(ScoredPick::score, ScoreCalculator.RANKING)
		// Only decides the display order of films that are fully tied.
		.thenComparing(scored -> scored.pick().getTitle(), String.CASE_INSENSITIVE_ORDER);

	private static final long NO_PICKER = -1;

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final PickRepository picks;

	private final RatingRepository ratings;

	private final GoldenTicketRepository tickets;

	public RatingService(MovieGroupRepository groups, MemberRepository members, PickRepository picks,
			RatingRepository ratings, GoldenTicketRepository tickets) {
		this.groups = groups;
		this.members = members;
		this.picks = picks;
		this.ratings = ratings;
		this.tickets = tickets;
	}

	// Creates or updates the caller's rating of one film. Nobody rates their own pick.
	@Transactional
	public RatingResponse rate(Member member, Long pickId, RatingRequest request) {
		Pick pick = picks.findById(pickId)
			.filter(found -> !found.isVetoed() && found.getGroup().getId().equals(member.getGroup().getId()))
			.orElseThrow(() -> ApiException.notFound("No such pick"));
		// Locked so two "last" ratings arriving together cannot both miss that rating is complete.
		MovieGroup group = groups.findWithLockById(pick.getGroup().getId())
			.orElseThrow(() -> ApiException.notFound("No such pick"));
		if (group.getStatus() != GroupStatus.WATCHING) {
			throw ApiException.conflict(group.getStatus() == GroupStatus.LOBBY ? "The marathon has not started yet"
					: "Rating is closed");
		}

		if (pick.getMember() != null && pick.getMember().getId().equals(member.getId())) {
			throw ApiException.conflict("You cannot rate your own pick");
		}

		Rating rating = ratings.findByPickIdAndMemberId(pickId, member.getId()).orElse(null);
		if (rating == null) {
			rating = new Rating(pick, member, request.scariness(), request.atmosphere(), request.story(),
					request.acting(), request.enjoyment());
		}
		else {
			rating.setScores(request.scariness(), request.atmosphere(), request.story(), request.acting(),
					request.enjoyment());
		}
		// saveAndFlush so the count below includes this rating.
		rating = ratings.saveAndFlush(rating);

		advance(group);
		return RatingResponse.from(rating);
	}

	// Moves the group on when a stage is complete: from WATCHING to TICKETS once every member
	// has rated every film except their own, and from TICKETS to DONE once every member has
	// given their golden ticket. Called after a rating, a ticket, or a member leaving.
	// The caller must be inside a transaction that holds the group's row lock.
	public void advance(MovieGroup group) {
		long memberCount = members.countByGroupId(group.getId());
		if (group.getStatus() == GroupStatus.WATCHING) {
			long filmCount = picks.countByGroupIdAndVetoedFalse(group.getId());
			// Each film whose picker is still here needs one rating fewer: the picker's own.
			long ownFilms = picks.countByGroupIdAndVetoedFalseAndMemberIsNotNull(group.getId());
			long expected = memberCount * filmCount - ownFilms;
			if (ratings.countByPickGroupId(group.getId()) >= expected) {
				endRating(group);
			}
		}
		else if (group.getStatus() == GroupStatus.TICKETS && tickets.countByGiverGroupId(group.getId()) >= memberCount) {
			group.setStatus(GroupStatus.DONE);
		}
	}

	// A ticket needs someone else to give it to, so a group of one skips that stage.
	public void endRating(MovieGroup group) {
		boolean canGiveTickets = members.countByGroupId(group.getId()) >= 2;
		group.setStatus(canGiveTickets ? GroupStatus.TICKETS : GroupStatus.DONE);
	}

	@Transactional(readOnly = true)
	public ResultsResponse results(MovieGroup callerGroup) {
		MovieGroup group = groups.findById(callerGroup.getId())
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() != GroupStatus.DONE) {
			throw ApiException.conflict("Results are not ready until rating and golden tickets are finished");
		}
		Map<Long, List<Scores>> scoresByPick = ratings.findByPickGroupId(group.getId())
			.stream()
			.collect(Collectors.groupingBy(rating -> rating.getPick().getId(),
					Collectors.mapping(RatingService::toScores, Collectors.toList())));

		// The ranking on ratings alone.
		List<ScoredPick> plain = picks.findByGroupIdAndVetoedFalse(group.getId())
			.stream()
			.map(pick -> new ScoredPick(pick,
					ScoreCalculator.score(pickerIdOf(pick), scoresByPick.getOrDefault(pick.getId(), List.of())), 0))
			.toList();

		// The golden ticket goes to a person; the boost goes to that person's film.
		List<Member> groupMembers = members.findByGroupIdOrderByIdAsc(group.getId());
		List<Long> recipientIds = tickets.findByGiverGroupId(group.getId())
			.stream()
			.map(ticket -> ticket.getRecipient().getId())
			.toList();
		Map<Long, Double> boostByMember = ScoreCalculator
			.ticketBoosts(groupMembers.stream().map(Member::getId).toList(), recipientIds);
		List<ScoredPick> boosted = plain.stream().map(scored -> {
			double boost = boostByMember.getOrDefault(pickerIdOf(scored.pick()), 0.0);
			return new ScoredPick(scored.pick(), ScoreCalculator.withBoost(scored.score(), boost), boost);
		}).toList();

		List<String> winners = groupMembers.stream()
			.filter(member -> boostByMember.containsKey(member.getId()))
			.map(Member::getNickname)
			.toList();
		double boostEach = boostByMember.values().stream().findFirst().orElse(0.0);
		return new ResultsResponse(group.getJoinCode(), group.getName(), rank(boosted), rank(plain),
				new GoldenTicketResult(winners, boostEach));
	}

	private static List<FilmResult> rank(List<ScoredPick> scoredPicks) {
		List<ScoredPick> ranked = scoredPicks.stream().sorted(RANKING).toList();
		List<FilmResult> films = new ArrayList<>();
		for (int i = 0; i < ranked.size(); i++) {
			ScoredPick current = ranked.get(i);
			// Films tied on both score and enjoyment share a rank.
			boolean tiedWithPrevious = i > 0
					&& ScoreCalculator.RANKING.compare(ranked.get(i - 1).score(), current.score()) == 0;
			int rank = tiedWithPrevious ? films.get(i - 1).rank() : i + 1;
			Pick pick = current.pick();
			films.add(new FilmResult(rank, pick.getId(), pick.getTitle(), pick.getReleaseYear(), pick.getPosterUrl(),
					round(current.score().score()), round(current.score().enjoyment()), current.score().raterCount(),
					current.boost()));
		}
		return films;
	}

	// A film whose picker has left has nobody to exclude; no real member has this id.
	private static long pickerIdOf(Pick pick) {
		return pick.getMember() == null ? NO_PICKER : pick.getMember().getId();
	}

	private static Scores toScores(Rating rating) {
		return new Scores(rating.getMember().getId(), rating.getScariness(), rating.getAtmosphere(), rating.getStory(),
				rating.getActing(), rating.getEnjoyment());
	}

	// Ranking uses the exact values; rounding to two decimals is only for display.
	private static Double round(Double value) {
		return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

}
