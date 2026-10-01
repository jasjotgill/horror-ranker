package io.github.jasjotgill.horror_ranker.dto;

import io.github.jasjotgill.horror_ranker.domain.Rating;

// A member's own rating of one film. Only ever sent back to the member who gave it.
public record RatingResponse(Long pickId, int scariness, int atmosphere, int story, int acting, int enjoyment) {

	public static RatingResponse from(Rating rating) {
		return new RatingResponse(rating.getPick().getId(), rating.getScariness(), rating.getAtmosphere(),
				rating.getStory(), rating.getActing(), rating.getEnjoyment());
	}

}
