package io.github.jasjotgill.horror_ranker.dto;

// The caller's private view of themselves. pickVetoed is true when their last pick was
// flagged as seen and they have not chosen again; only they are ever told.
public record YourStatus(String nickname, PickResponse pick, boolean pickVetoed) {
}
