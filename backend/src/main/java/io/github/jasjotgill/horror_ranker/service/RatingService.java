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
import io.github.jasjotgill.horror_ranker.dto.RatingRequest;
import io.github.jasjotgill.horror_ranker.dto.RatingResponse;
import io.github.jasjotgill.horror_ranker.dto.ResultsResponse;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
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

	private record ScoredPick(Pick pick, FilmScore score) {
	}

	private static final Comparator<ScoredPick> RANKING = Comparator
		.comparing(ScoredPick::score, ScoreCalculator.RANKING)
		// Only decides the display order of films that are fully tied.
		.thenComparing(scored -> scored.pick().getTitle(), String.CASE_INSENSITIVE_ORDER);

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final PickRepository picks;

	private final RatingRepository ratings;

	public RatingService(MovieGroupRepository groups, MemberRepository members, PickRepository picks,
			RatingRepository ratings) {
		this.groups = groups;
		this.members = members;
		this.picks = picks;
		this.ratings = ratings;
	}

	// Creates or updates the caller's rating of one film. A member rating their own pick gets
	// exactly the same treatment and reply as anyone else, so neither the response nor the
	// screen gives the picker away; ScoreCalculator leaves that rating out of the score.
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

		long expected = (long) members.countByGroupId(group.getId())
				* picks.findByGroupIdAndVetoedFalse(group.getId()).size();
		if (ratings.countByPickGroupId(group.getId()) >= expected) {
			group.setStatus(GroupStatus.DONE);
		}
		return RatingResponse.from(rating);
	}

	@Transactional(readOnly = true)
	public ResultsResponse results(MovieGroup callerGroup) {
		MovieGroup group = groups.findById(callerGroup.getId())
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() != GroupStatus.DONE) {
			throw ApiException.conflict("Results are not ready until rating is finished");
		}
		Map<Long, List<Scores>> scoresByPick = ratings.findByPickGroupId(group.getId())
			.stream()
			.collect(Collectors.groupingBy(rating -> rating.getPick().getId(),
					Collectors.mapping(RatingService::toScores, Collectors.toList())));

		List<ScoredPick> ranked = picks.findByGroupIdAndVetoedFalse(group.getId())
			.stream()
			.map(pick -> new ScoredPick(pick,
					ScoreCalculator.score(pick.getMember().getId(), scoresByPick.getOrDefault(pick.getId(), List.of()))))
			.sorted(RANKING)
			.toList();

		List<FilmResult> films = new ArrayList<>();
		for (int i = 0; i < ranked.size(); i++) {
			ScoredPick current = ranked.get(i);
			// Films tied on both score and enjoyment share a rank.
			boolean tiedWithPrevious = i > 0
					&& ScoreCalculator.RANKING.compare(ranked.get(i - 1).score(), current.score()) == 0;
			int rank = tiedWithPrevious ? films.get(i - 1).rank() : i + 1;
			Pick pick = current.pick();
			films.add(new FilmResult(rank, pick.getId(), pick.getTitle(), pick.getReleaseYear(), pick.getPosterUrl(),
					round(current.score().score()), round(current.score().enjoyment()), current.score().raterCount()));
		}
		return new ResultsResponse(group.getJoinCode(), group.getName(), films);
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
