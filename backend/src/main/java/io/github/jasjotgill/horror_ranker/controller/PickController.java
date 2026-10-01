package io.github.jasjotgill.horror_ranker.controller;

import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.dto.PickRequest;
import io.github.jasjotgill.horror_ranker.dto.PickResponse;
import io.github.jasjotgill.horror_ranker.service.MemberAuthenticator;
import io.github.jasjotgill.horror_ranker.service.PickService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PickController {

	private final PickService pickService;

	private final MemberAuthenticator authenticator;

	public PickController(PickService pickService, MemberAuthenticator authenticator) {
		this.pickService = pickService;
		this.authenticator = authenticator;
	}

	// Submits the caller's pick, replacing their current one if they have one.
	@PostMapping("/groups/{code}/picks")
	public PickResponse submitPick(@PathVariable String code, @RequestBody PickRequest request,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(code, token);
		return pickService.submitPick(caller, request);
	}

	@PostMapping("/picks/{id}/veto")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void veto(@PathVariable Long id,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(token);
		pickService.veto(caller, id);
	}

}
