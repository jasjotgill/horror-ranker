package io.github.jasjotgill.horror_ranker.controller;

import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.dto.RatingRequest;
import io.github.jasjotgill.horror_ranker.dto.RatingResponse;
import io.github.jasjotgill.horror_ranker.dto.ResultsResponse;
import io.github.jasjotgill.horror_ranker.service.MemberAuthenticator;
import io.github.jasjotgill.horror_ranker.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RatingController {

	private final RatingService ratingService;

	private final MemberAuthenticator authenticator;

	public RatingController(RatingService ratingService, MemberAuthenticator authenticator) {
		this.ratingService = ratingService;
		this.authenticator = authenticator;
	}

	// PUT because sending the same rating twice leaves the same result: it creates or overwrites.
	@PutMapping("/picks/{id}/rating")
	public RatingResponse rate(@PathVariable Long id, @Valid @RequestBody RatingRequest request,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(token);
		return ratingService.rate(caller, id, request);
	}

	@GetMapping("/groups/{code}/results")
	public ResultsResponse results(@PathVariable String code,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		Member caller = authenticator.requireMember(code, token);
		return ratingService.results(caller.getGroup());
	}

}
