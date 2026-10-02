package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

// films is the final ranking, with the golden ticket applied.
// filmsWithoutTicket is the ranking on ratings alone, for comparison.
public record ResultsResponse(String code, String name, List<FilmResult> films, List<FilmResult> filmsWithoutTicket,
		GoldenTicketResult goldenTicket) {
}
