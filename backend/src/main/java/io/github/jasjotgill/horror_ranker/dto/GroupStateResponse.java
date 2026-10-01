package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;

// Body of GET /api/groups/{code}. Members are nicknames only: no ids, no tokens.
// pickedCount is a number, not a per-member flag: showing who has picked next to a
// list of picks would let anyone watching the lobby match the two up.
public record GroupStateResponse(String code, String name, GroupStatus status, List<String> members,
		List<PickResponse> picks, int pickedCount, YourStatus you) {
}
