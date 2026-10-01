package io.github.jasjotgill.horror_ranker.dto;

// Either tmdbId alone (a search result) or title and year (manual entry), never both.
// The rule spans several fields, so PickService checks it rather than annotations here.
public record PickRequest(Long tmdbId, String title, Integer year, String posterUrl) {
}
