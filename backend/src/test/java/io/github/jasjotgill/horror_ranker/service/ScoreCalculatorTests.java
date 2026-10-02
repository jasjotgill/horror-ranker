package io.github.jasjotgill.horror_ranker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.github.jasjotgill.horror_ranker.service.ScoreCalculator.FilmScore;
import io.github.jasjotgill.horror_ranker.service.ScoreCalculator.Scores;
import org.junit.jupiter.api.Test;

class ScoreCalculatorTests {

	private static final long PICKER = 1;

	@Test
	void averagesEachRaterThenAveragesAcrossRaters() {
		// Rater 2 averages 6.0, rater 3 averages (8+8+8+8+10)/5 = 8.4, so the film scores 7.2.
		FilmScore score = ScoreCalculator.score(PICKER,
				List.of(new Scores(2, 6, 6, 6, 6, 6), new Scores(3, 8, 8, 8, 8, 10)));

		assertThat(score.score()).isCloseTo(7.2, within(1e-9));
		assertThat(score.enjoyment()).isCloseTo(8.0, within(1e-9));
		assertThat(score.raterCount()).isEqualTo(2);
	}

	@Test
	void excludesThePickersOwnRating() {
		// The picker gives their own film straight 10s. It must not move the score.
		FilmScore score = ScoreCalculator.score(PICKER,
				List.of(new Scores(PICKER, 10, 10, 10, 10, 10), new Scores(2, 4, 4, 4, 4, 4)));

		assertThat(score.score()).isCloseTo(4.0, within(1e-9));
		assertThat(score.enjoyment()).isCloseTo(4.0, within(1e-9));
		assertThat(score.raterCount()).isEqualTo(1);
	}

	@Test
	void hasNoScoreWhenOnlyThePickerRated() {
		FilmScore score = ScoreCalculator.score(PICKER, List.of(new Scores(PICKER, 10, 10, 10, 10, 10)));

		assertThat(score.score()).isNull();
		assertThat(score.enjoyment()).isNull();
		assertThat(score.raterCount()).isZero();
	}

	@Test
	void ranksHigherScoreFirst() {
		FilmScore better = new FilmScore(8.0, 5.0, 2);
		FilmScore worse = new FilmScore(7.9, 10.0, 2);

		assertThat(Stream.of(worse, better).sorted(ScoreCalculator.RANKING)).containsExactly(better, worse);
	}

	@Test
	void breaksTiesWithHigherEnjoyment() {
		// Both films score 7.0 overall; only enjoyment separates them.
		FilmScore lessEnjoyed = ScoreCalculator.score(PICKER, List.of(new Scores(2, 8, 8, 7, 7, 5)));
		FilmScore moreEnjoyed = ScoreCalculator.score(PICKER, List.of(new Scores(2, 6, 6, 7, 7, 9)));

		assertThat(lessEnjoyed.score()).isEqualTo(moreEnjoyed.score());
		assertThat(Stream.of(lessEnjoyed, moreEnjoyed).sorted(ScoreCalculator.RANKING)).containsExactly(moreEnjoyed,
				lessEnjoyed);
	}

	@Test
	void givesTheWholeTicketBoostToAClearWinner() {
		// Five people; member 2 receives three tickets, member 3 two.
		Map<Long, Double> boosts = ScoreCalculator.ticketBoosts(List.of(1L, 2L, 3L, 4L, 5L),
				List.of(2L, 2L, 2L, 3L, 3L));

		assertThat(boosts).containsExactly(Map.entry(2L, 1.0));
	}

	@Test
	void splitsTheTicketBoostBetweenTiedWinners() {
		// Members 2 and 3 receive two tickets each, member 1 one.
		Map<Long, Double> boosts = ScoreCalculator.ticketBoosts(List.of(1L, 2L, 3L, 4L, 5L),
				List.of(2L, 2L, 3L, 3L, 1L));

		assertThat(boosts).containsOnly(Map.entry(2L, 0.5), Map.entry(3L, 0.5));
	}

	@Test
	void givesNoTicketBoostWhenEveryoneIsTied() {
		// Each of the three receives exactly one ticket.
		assertThat(ScoreCalculator.ticketBoosts(List.of(1L, 2L, 3L), List.of(2L, 3L, 1L))).isEmpty();
		// Nobody gave a ticket at all.
		assertThat(ScoreCalculator.ticketBoosts(List.of(1L, 2L, 3L), List.of())).isEmpty();
	}

	@Test
	void capsABoostedScoreAtTen() {
		FilmScore boosted = ScoreCalculator.withBoost(new FilmScore(9.6, 8.0, 4), 1.0);

		assertThat(boosted.score()).isEqualTo(10.0);
		assertThat(boosted.enjoyment()).isEqualTo(8.0);
		assertThat(ScoreCalculator.withBoost(new FilmScore(7.0, 8.0, 4), 0.5).score()).isCloseTo(7.5, within(1e-9));
	}

	@Test
	void leavesAnUnratedFilmUnratedWhenBoosted() {
		assertThat(ScoreCalculator.withBoost(new FilmScore(null, null, 0), 1.0).score()).isNull();
	}

	@Test
	void ranksUnratedFilmsLast() {
		FilmScore unrated = new FilmScore(null, null, 0);
		FilmScore lowest = new FilmScore(1.0, 1.0, 1);

		assertThat(Stream.of(unrated, lowest).sorted(ScoreCalculator.RANKING)).containsExactly(lowest, unrated);
	}

}
