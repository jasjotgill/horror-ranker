package io.github.jasjotgill.horror_ranker.dto;

import io.github.jasjotgill.horror_ranker.domain.Pick;

// What a client may see of a pick. There is deliberately no member field:
// leaving it out of the record is what keeps picks anonymous.
public record PickResponse(Long id, String title, Integer releaseYear, String posterUrl, Integer watchOrder) {

	public static PickResponse from(Pick pick) {
		return new PickResponse(pick.getId(), pick.getTitle(), pick.getReleaseYear(), pick.getPosterUrl(),
				pick.getWatchOrder());
	}

}
