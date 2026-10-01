package io.github.jasjotgill.horror_ranker.dto;

import java.util.Map;

// The one shape every error has. "fields" maps a request field to its problem
// and is empty unless validation failed.
public record ErrorResponse(int status, String message, Map<String, String> fields) {

	public ErrorResponse(int status, String message) {
		this(status, message, Map.of());
	}

}
