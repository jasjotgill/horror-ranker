package io.github.jasjotgill.horror_ranker.service;

import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Turns the X-Member-Token header into a Member, or ends the request.
@Component
public class MemberAuthenticator {

	public static final String TOKEN_HEADER = "X-Member-Token";

	private final MemberRepository members;

	public MemberAuthenticator(MemberRepository members) {
		this.members = members;
	}

	// 401 when we cannot tell who is calling, 403 when we can but they are in a different group.
	// The transaction keeps the session open while member.getGroup() is lazily loaded.
	@Transactional(readOnly = true)
	public Member requireMember(String code, String token) {
		Member member = requireMember(token);
		if (!member.getGroup().getJoinCode().equalsIgnoreCase(code.strip())) {
			throw ApiException.forbidden("You are not a member of this group");
		}
		return member;
	}

	// For endpoints with no group code in the URL: any valid member will do.
	@Transactional(readOnly = true)
	public Member requireMember(String token) {
		if (token == null || token.isBlank()) {
			throw ApiException.unauthorized("Missing " + TOKEN_HEADER + " header");
		}
		return members.findByToken(token).orElseThrow(() -> ApiException.unauthorized("Unknown member token"));
	}

}
