package io.github.jasjotgill.horror_ranker.dto;

// One row of the results page. score and enjoyment are null when nobody eligible rated the film.
// ticketBoost is what the golden ticket added to this film's score; 0 for most films.
// There is no picker here either.
public record FilmResult(int rank, Long pickId, String title, Integer releaseYear, String posterUrl, Double score,
		Double enjoyment, int raterCount, double ticketBoost) {
}
