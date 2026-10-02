package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

// The caller's private view of themselves.
// host: whether the caller may start the marathon and remove people.
// rejoinCode: the caller's own code for getting back in from another browser or device.
// pickVetoed: true when their last pick was removed because two people had seen it and
// they have not chosen again; only they are ever told.
// seenPickIds: the picks the caller has flagged as seen.
// ratings: the caller's own, so the rating screen can show them again after a refresh.
// ticketFor: the nickname the caller gave their golden ticket to, or null.
public record YourStatus(String nickname, boolean host, String rejoinCode, PickResponse pick, boolean pickVetoed,
		List<Long> seenPickIds, List<RatingResponse> ratings, String ticketFor) {
}
