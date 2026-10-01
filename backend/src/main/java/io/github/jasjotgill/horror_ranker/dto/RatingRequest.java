package io.github.jasjotgill.horror_ranker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Integer, not int, so a missing field is null and fails @NotNull instead of silently becoming 0.
public record RatingRequest(@NotNull @Min(1) @Max(10) Integer scariness, @NotNull @Min(1) @Max(10) Integer atmosphere,
		@NotNull @Min(1) @Max(10) Integer story, @NotNull @Min(1) @Max(10) Integer acting,
		@NotNull @Min(1) @Max(10) Integer enjoyment) {
}
