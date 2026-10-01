package io.github.jasjotgill.horror_ranker.service;

import java.util.Locale;

public final class TitleNormalizer {

	private TitleNormalizer() {
	}

	// "The Thing!" and "the thing" compare equal: lowercase, with every run of
	// punctuation or spaces reduced to a single space.
	public static String normalize(String title) {
		return title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
	}

}
