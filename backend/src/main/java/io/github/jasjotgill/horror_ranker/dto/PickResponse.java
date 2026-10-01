package io.github.jasjotgill.horror_ranker.dto;

import io.github.jasjotgill.horror_ranker.domain.Pick;

// What a client may see of a pick. There is deliberately no member field:
// leaving it out of the record is what keeps picks anonymous.
// seenCount is how many members have said "I've seen it", never who.
public record PickResponse(Long id, String title, Integer releaseYear, String posterUrl, Integer watchOrder,
		int seenCount) {

	public static PickResponse from(Pick pick, int seenCount) {
		return new PickResponse(pick.getId(), pick.getTitle(), pick.getReleaseYear(), pick.getPosterUrl(),
				pick.getWatchOrder(), seenCount);
	}

}
