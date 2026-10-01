package io.github.jasjotgill.horror_ranker.dto;

// One search hit, cut down to what the pick screen needs. year and posterUrl may be null.
public record MovieSearchResult(Long id, String title, Integer year, String posterUrl) {
}
