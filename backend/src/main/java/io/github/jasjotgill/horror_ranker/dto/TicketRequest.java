package io.github.jasjotgill.horror_ranker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Who the caller gives their golden ticket to, by nickname.
public record TicketRequest(@NotBlank @Size(max = 30) String nickname) {
}
