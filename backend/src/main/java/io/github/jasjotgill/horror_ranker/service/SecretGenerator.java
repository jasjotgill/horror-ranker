package io.github.jasjotgill.horror_ranker.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class SecretGenerator {

	// No O, 0, I or 1: they are too easy to mix up when a code is read aloud.
	private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private static final int JOIN_CODE_LENGTH = 4;

	private static final int REJOIN_CODE_LENGTH = 6;

	private static final int TOKEN_BYTES = 32;

	// SecureRandom, not Random: its output cannot be predicted from earlier values.
	private final SecureRandom random = new SecureRandom();

	public String newJoinCode() {
		return newCode(JOIN_CODE_LENGTH);
	}

	// Longer than a join code because it stands in for a login, not just an invitation.
	public String newRejoinCode() {
		return newCode(REJOIN_CODE_LENGTH);
	}

	private String newCode(int length) {
		StringBuilder code = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
		}
		return code.toString();
	}

	// 32 random bytes as 43 URL-safe characters.
	public String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
