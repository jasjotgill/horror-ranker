package io.github.jasjotgill.horror_ranker.tmdb;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.jasjotgill.horror_ranker.dto.MovieSearchResult;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// The only class that talks to TMDB. The token stays on the server; browsers call our API instead.
@Component
public class TmdbClient {

	private static final int MAX_RESULTS = 10;

	private final RestClient restClient;

	private final String imageBaseUrl;

	private final boolean configured;

	public TmdbClient(RestClient.Builder builder, @Value("${tmdb.base-url}") String baseUrl,
			@Value("${tmdb.image-base-url}") String imageBaseUrl, @Value("${tmdb.api-token}") String apiToken) {
		// Without a timeout a slow TMDB would hang our request thread indefinitely.
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		this.restClient = builder.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
			.build();
		this.imageBaseUrl = imageBaseUrl;
		this.configured = !apiToken.isBlank();
	}

	public List<MovieSearchResult> search(String query) {
		if (!configured) {
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Movie search is not configured on the server");
		}
		SearchPage page;
		try {
			page = restClient.get()
				.uri(uri -> uri.path("/search/movie")
					.queryParam("query", query)
					.queryParam("include_adult", false)
					.build())
				.retrieve()
				.body(SearchPage.class);
		}
		catch (RestClientException ex) {
			// Covers timeouts, connection failures and non-2xx answers from TMDB.
			throw new ApiException(HttpStatus.BAD_GATEWAY, "Movie search is unavailable right now");
		}
		if (page == null || page.results() == null) {
			return List.of();
		}
		return page.results().stream().limit(MAX_RESULTS).map(this::toResult).toList();
	}

	private MovieSearchResult toResult(TmdbMovie movie) {
		String posterUrl = movie.posterPath() == null ? null : imageBaseUrl + movie.posterPath();
		return new MovieSearchResult(movie.id(), movie.title(), yearOf(movie.releaseDate()), posterUrl);
	}

	// TMDB sends "2018-06-07", or "" when the date is unknown.
	private static Integer yearOf(String releaseDate) {
		if (releaseDate == null || releaseDate.length() < 4) {
			return null;
		}
		try {
			return Integer.valueOf(releaseDate.substring(0, 4));
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	// TMDB's JSON, mapped only as far as we read it. Private so its shape cannot leak into our API.
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record SearchPage(List<TmdbMovie> results) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TmdbMovie(Long id, String title, @JsonProperty("release_date") String releaseDate,
			@JsonProperty("poster_path") String posterPath) {
	}

}
