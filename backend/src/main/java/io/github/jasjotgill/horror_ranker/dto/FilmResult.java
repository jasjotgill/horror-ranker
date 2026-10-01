package io.github.jasjotgill.horror_ranker.dto;

// One row of the results page. score and enjoyment are null when nobody eligible rated the film.
// There is no picker here either.
public record FilmResult(int rank, Long pickId, String title, Integer releaseYear, String posterUrl, Double score,
		Double enjoyment, int raterCount) {
}
