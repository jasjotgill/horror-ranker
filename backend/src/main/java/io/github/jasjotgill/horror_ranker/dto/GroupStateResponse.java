package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;

// Body of GET /api/groups/{code}. Members are nicknames only: no ids, no tokens.
public record GroupStateResponse(String code, String name, GroupStatus status, List<String> members,
		List<PickResponse> picks) {
}
