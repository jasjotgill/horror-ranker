package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

// The caller's private view of themselves. pickVetoed is true when their last pick was
// flagged as seen and they have not chosen again; only they are ever told.
// ratings are the caller's own, so the rating screen can show them again after a refresh.
public record YourStatus(String nickname, PickResponse pick, boolean pickVetoed, List<RatingResponse> ratings) {
}
