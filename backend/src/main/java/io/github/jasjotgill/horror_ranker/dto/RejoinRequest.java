package io.github.jasjotgill.horror_ranker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejoinRequest(@NotBlank @Size(max = 8) String rejoinCode) {
}
