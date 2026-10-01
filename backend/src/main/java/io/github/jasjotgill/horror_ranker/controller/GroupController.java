package io.github.jasjotgill.horror_ranker.controller;

import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.dto.CreateGroupRequest;
import io.github.jasjotgill.horror_ranker.dto.GroupStateResponse;
import io.github.jasjotgill.horror_ranker.dto.JoinGroupRequest;
import io.github.jasjotgill.horror_ranker.dto.MembershipResponse;
import io.github.jasjotgill.horror_ranker.service.GroupService;
import io.github.jasjotgill.horror_ranker.service.MemberAuthenticator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Controllers only translate HTTP to method calls; the rules live in the services.
@RestController
@RequestMapping("/api/groups")
public class GroupController {

	private final GroupService groupService;

	private final MemberAuthenticator authenticator;

	public GroupController(GroupService groupService, MemberAuthenticator authenticator) {
		this.groupService = groupService;
		this.authenticator = authenticator;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
		return groupService.createGroup(request.name(), request.nickname());
	}

	@PostMapping("/{code}/members")
	@ResponseStatus(HttpStatus.CREATED)
	public MembershipResponse joinGroup(@PathVariable String code, @Valid @RequestBody JoinGroupRequest request) {
		return groupService.joinGroup(code, request.nickname());
	}

	// required = false so a missing header reaches the authenticator and gets our JSON 401.
	@GetMapping("/{code}")
	public GroupStateResponse getState(@PathVariable String code,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(code, token);
		return groupService.getState(code, caller);
	}

	@PostMapping("/{code}/start")
	public GroupStateResponse start(@PathVariable String code,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(code, token);
		return groupService.start(code, caller);
	}

}
