package io.github.jasjotgill.horror_ranker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Members are identified to clients by nickname only, so that is how the host names one.
public record KickRequest(@NotBlank @Size(max = 30) String nickname) {
}
