package io.github.jasjotgill.horror_ranker.service;

import java.util.Comparator;
import java.util.List;

// The scoring rule from the handout, with no Spring or database in it so it can be unit tested.
public final class ScoreCalculator {

	private static final int CATEGORIES = 5;

	private ScoreCalculator() {
	}

	// One member's five scores for one film.
	public record Scores(long raterId, int scariness, int atmosphere, int story, int acting, int enjoyment) {

		int total() {
			return scariness + atmosphere + story + acting + enjoyment;
		}

	}

	// score and enjoyment are null when no eligible rater scored the film.
	public record FilmScore(Double score, Double enjoyment, int raterCount) {
	}

	// Highest score first, ties broken by higher enjoyment, unrated films last.
	public static final Comparator<FilmScore> RANKING = Comparator
		.comparing(FilmScore::score, Comparator.nullsFirst(Comparator.<Double>naturalOrder()))
		.thenComparing(FilmScore::enjoyment, Comparator.nullsFirst(Comparator.<Double>naturalOrder()))
		.reversed();

	// Each rater's five scores are averaged, then those averages are averaged across raters.
	// Every rater has exactly five scores, so that equals all points divided by (5 x raters).
	// The picker's own rating is left out.
	public static FilmScore score(long pickerId, List<Scores> ratings) {
		List<Scores> eligible = ratings.stream().filter(rating -> rating.raterId() != pickerId).toList();
		if (eligible.isEmpty()) {
			return new FilmScore(null, null, 0);
		}
		int raters = eligible.size();
		int points = eligible.stream().mapToInt(Scores::total).sum();
		int enjoymentPoints = eligible.stream().mapToInt(Scores::enjoyment).sum();
		return new FilmScore((double) points / (CATEGORIES * raters), (double) enjoymentPoints / raters, raters);
	}

}
