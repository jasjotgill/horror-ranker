package io.github.jasjotgill.horror_ranker.controller;

import java.util.List;

import io.github.jasjotgill.horror_ranker.dto.MovieSearchResult;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.service.MemberAuthenticator;
import io.github.jasjotgill.horror_ranker.tmdb.TmdbClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

	private static final int MAX_QUERY_LENGTH = 100;

	private final TmdbClient tmdb;

	private final MemberAuthenticator authenticator;

	public MovieController(TmdbClient tmdb, MemberAuthenticator authenticator) {
		this.tmdb = tmdb;
		this.authenticator = authenticator;
	}

	// Members only, so the endpoint is not an open proxy that spends our TMDB quota.
	@GetMapping("/search")
	public List<MovieSearchResult> search(@RequestParam(name = "q", defaultValue = "") String q,
			@RequestHeader(value = MemberAuthenticator.TOKEN_HEADER, required = false) String token) {
		authenticator.requireMember(token);
		String query = q.strip();
		if (query.isEmpty() || query.length() > MAX_QUERY_LENGTH) {
			throw ApiException.badRequest("q must be between 1 and " + MAX_QUERY_LENGTH + " characters");
		}
		return tmdb.search(query);
	}

}
