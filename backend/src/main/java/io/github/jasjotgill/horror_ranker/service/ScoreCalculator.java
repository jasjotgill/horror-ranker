package io.github.jasjotgill.horror_ranker.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// The scoring rule from the handout, with no Spring or database in it so it can be unit tested.
public final class ScoreCalculator {

	private static final int CATEGORIES = 5;

	// What the golden ticket is worth in total.
	public static final double TICKET_BOOST = 1.0;

	private static final double MAX_SCORE = 10.0;

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

	// Works out what the golden ticket adds to whose film. Returns member id -> boost.
	// The member with the most tickets gets the whole boost. Members tied for the most share it
	// equally. If everyone is tied (which includes nobody giving a ticket), nobody gets anything.
	public static Map<Long, Double> ticketBoosts(Collection<Long> memberIds, Collection<Long> recipientIds) {
		Map<Long, Long> received = recipientIds.stream()
			.collect(Collectors.groupingBy(id -> id, Collectors.counting()));
		long most = memberIds.stream().mapToLong(id -> received.getOrDefault(id, 0L)).max().orElse(0);
		List<Long> winners = memberIds.stream().filter(id -> received.getOrDefault(id, 0L) == most).toList();
		if (most == 0 || winners.size() == memberIds.size()) {
			return Map.of();
		}
		double share = TICKET_BOOST / winners.size();
		return winners.stream().collect(Collectors.toMap(id -> id, id -> share));
	}

	// Adds a boost to a film's score, never past the top of the scale. An unrated film stays unrated.
	public static FilmScore withBoost(FilmScore film, double boost) {
		if (film.score() == null || boost == 0) {
			return film;
		}
		return new FilmScore(Math.min(MAX_SCORE, film.score() + boost), film.enjoyment(), film.raterCount());
	}

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
