package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

public record ResultsResponse(String code, String name, List<FilmResult> films) {
}
