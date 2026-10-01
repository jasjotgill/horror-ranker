package io.github.jasjotgill.horror_ranker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// The creator is also the first member, so creating a group needs a nickname too.
public record CreateGroupRequest(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 30) String nickname) {
}
