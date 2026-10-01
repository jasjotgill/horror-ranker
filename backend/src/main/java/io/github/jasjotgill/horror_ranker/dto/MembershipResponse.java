package io.github.jasjotgill.horror_ranker.dto;

import io.github.jasjotgill.horror_ranker.domain.Member;

// Returned once, on create and on join. This is the only response that ever contains a token.
public record MembershipResponse(String code, String nickname, String token) {

	public static MembershipResponse from(Member member) {
		return new MembershipResponse(member.getGroup().getJoinCode(), member.getNickname(), member.getToken());
	}

}
