package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;

// Body of GET /api/groups/{code}. Members are nicknames only: no ids, no tokens.
// host is the nickname of the member who can start the marathon and remove people.
// pickedCount and ticketsGiven are numbers, not per-member flags: showing who has picked
// next to a list of picks would let anyone watching the lobby match the two up.
public record GroupStateResponse(String code, String name, GroupStatus status, String host, List<String> members,
		List<PickResponse> picks, int pickedCount, int ticketsGiven, YourStatus you) {
}
