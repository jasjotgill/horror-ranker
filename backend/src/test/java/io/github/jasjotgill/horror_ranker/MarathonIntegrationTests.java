package io.github.jasjotgill.horror_ranker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.jayway.jsonpath.JsonPath;
import io.github.jasjotgill.horror_ranker.dto.MovieSearchResult;
import io.github.jasjotgill.horror_ranker.tmdb.TmdbClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

// Starts the whole application against a real PostGIS container, with Flyway applying V1.
// Requests go through MockMvc, which runs controllers, validation and error handling
// without opening a network port.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MarathonIntegrationTests {

	private static final Set<String> PICK_FIELDS = Set.of("id", "title", "releaseYear", "posterUrl", "watchOrder",
			"seenCount");

	@Autowired
	private MockMvc mvc;

	// Replaces the real client, so tests never call TMDB and need no API token.
	@MockitoBean
	private TmdbClient tmdb;

	@Test
	void playsAFullMarathonFromCreateToResults() throws Exception {
		Player jas = createGroup("Friday Frights", "Jas");
		Player sam = join(jas.code(), "Sam");
		Player ria = join(jas.code(), "Ria");

		int filmA = pickManually(jas, "Film A", 2001);
		int filmB = pickManually(sam, "Film B", 2002);
		int filmC = pickManually(ria, "Film C", 2003);

		// Only the host (the creator) can start.
		send(post("/api/groups/{code}/start", jas.code()), sam, null, 403);
		String started = send(post("/api/groups/{code}/start", jas.code()), jas, null, 200);
		assertThat(JsonPath.<String>read(started, "$.status")).isEqualTo("WATCHING");
		assertThat(JsonPath.<List<Integer>>read(started, "$.picks[*].watchOrder")).containsExactly(1, 2, 3);

		// The order was stored, so reading the group again must not reshuffle it.
		List<Integer> order = JsonPath.read(started, "$.picks[*].id");
		for (Player player : List.of(jas, sam, ria)) {
			assertThat(JsonPath.<List<Integer>>read(state(player), "$.picks[*].id")).isEqualTo(order);
		}

		// Nobody can rate their own pick.
		rateExpecting(409, jas, filmA, 10, 10, 10, 10, 10);

		// Every film averages 7.0 from the two people who did not pick it.
		rate(sam, filmA, 6, 6, 6, 6, 6);
		rate(ria, filmA, 8, 8, 8, 8, 8);

		rate(jas, filmB, 7, 7, 7, 7, 5);
		rate(ria, filmB, 7, 7, 7, 7, 9);

		rate(jas, filmC, 6, 6, 6, 6, 9);
		send(get("/api/groups/{code}/results", jas.code()), jas, null, 409);
		rate(sam, filmC, 7, 7, 7, 7, 9);

		// The last rating ends rating. Results wait for the golden tickets.
		assertThat(JsonPath.<String>read(state(jas), "$.status")).isEqualTo("TICKETS");
		send(get("/api/groups/{code}/results", jas.code()), jas, null, 409);

		// Nobody can give the ticket to themselves. Sam gets two tickets, Jas one.
		giveTicketExpecting(409, jas, "Jas");
		giveTicket(jas, "Ria");
		giveTicket(jas, "Sam");
		giveTicket(ria, "Sam");
		assertThat(JsonPath.<Integer>read(state(sam), "$.ticketsGiven")).isEqualTo(2);
		assertThat(JsonPath.<String>read(state(jas), "$.you.ticketFor")).isEqualTo("Sam");
		giveTicket(sam, "Jas");

		// The last ticket finishes the marathon.
		assertThat(JsonPath.<String>read(state(jas), "$.status")).isEqualTo("DONE");
		String results = send(get("/api/groups/{code}/results", jas.code()), sam, null, 200);

		// On ratings alone: Film C wins the three-way tie on enjoyment (9.0 against 7.0),
		// and A and B tie completely.
		assertThat(JsonPath.<List<String>>read(results, "$.filmsWithoutTicket[*].title")).containsExactly("Film C",
				"Film A", "Film B");
		assertThat(JsonPath.<List<Integer>>read(results, "$.filmsWithoutTicket[*].rank")).containsExactly(1, 2, 2);
		assertThat(JsonPath.<List<Double>>read(results, "$.filmsWithoutTicket[*].score")).containsExactly(7.0, 7.0,
				7.0);
		assertThat(JsonPath.<List<Double>>read(results, "$.filmsWithoutTicket[*].enjoyment")).containsExactly(9.0, 7.0,
				7.0);
		assertThat(JsonPath.<List<Integer>>read(results, "$.filmsWithoutTicket[*].raterCount")).containsExactly(2, 2,
				2);

		// Sam won the ticket outright, so Sam's Film B gains a full point and jumps from last to first.
		assertThat(JsonPath.<List<String>>read(results, "$.goldenTicket.winners")).containsExactly("Sam");
		assertThat(JsonPath.<Double>read(results, "$.goldenTicket.boost")).isEqualTo(1.0);
		assertThat(JsonPath.<List<String>>read(results, "$.films[*].title")).containsExactly("Film B", "Film C",
				"Film A");
		assertThat(JsonPath.<List<Integer>>read(results, "$.films[*].rank")).containsExactly(1, 2, 3);
		assertThat(JsonPath.<List<Double>>read(results, "$.films[*].score")).containsExactly(8.0, 7.0, 7.0);
		assertThat(JsonPath.<List<Double>>read(results, "$.films[*].ticketBoost")).containsExactly(1.0, 0.0, 0.0);
	}

	// The anonymity guarantee: a pick in the group state has exactly the film fields and a count,
	// and none of its values is a member's nickname.
	@Test
	void groupStateNeverRevealsWhoPickedWhat() throws Exception {
		Player zelda = createGroup("Secret Cinema", "Zelda");
		Player quentin = join(zelda.code(), "Quentin");
		pickManually(zelda, "Film One", 1999);
		pickManually(quentin, "Film Two", 2005);

		assertPicksAreAnonymous(state(zelda), "Zelda", "Quentin");
		assertPicksAreAnonymous(state(quentin), "Zelda", "Quentin");

		String started = send(post("/api/groups/{code}/start", zelda.code()), zelda, null, 200);
		assertPicksAreAnonymous(started, "Zelda", "Quentin");
	}

	@Test
	void acceptsAValidManualPick() throws Exception {
		Player jas = createGroup("Manual", "Jas");

		String pick = send(post("/api/groups/{code}/picks", jas.code()), jas, """
				{"title": "  Grandma's Cursed VHS  ", "year": 1994, "posterUrl": "https://example.com/vhs.jpg"}
				""", 200);

		assertThat(JsonPath.<String>read(pick, "$.title")).isEqualTo("Grandma's Cursed VHS");
		assertThat(JsonPath.<Integer>read(pick, "$.releaseYear")).isEqualTo(1994);
		assertThat(JsonPath.<String>read(pick, "$.posterUrl")).isEqualTo("https://example.com/vhs.jpg");
		assertThat(JsonPath.<Integer>read(state(jas), "$.you.pick.id")).isEqualTo(JsonPath.<Integer>read(pick, "$.id"));
	}

	@Test
	void rejectsAPickWithBothATmdbIdAndATitle() throws Exception {
		Player jas = createGroup("Both", "Jas");

		send(post("/api/groups/{code}/picks", jas.code()), jas, """
				{"tmdbId": 1091, "title": "The Thing", "year": 1982}
				""", 400);

		assertThat(JsonPath.<List<Object>>read(state(jas), "$.picks")).isEmpty();
	}

	@Test
	void blocksAManualDuplicateOfATmdbPick() throws Exception {
		given(tmdb.findById(1091L)).willReturn(Optional.of(new MovieSearchResult(1091L, "The Thing", 1982, null)));
		Player jas = createGroup("Duplicates", "Jas");
		Player sam = join(jas.code(), "Sam");
		send(post("/api/groups/{code}/picks", jas.code()), jas, """
				{"tmdbId": 1091}
				""", 200);

		// Different capitalisation, punctuation and spacing, but the same film.
		String error = send(post("/api/groups/{code}/picks", sam.code()), sam, """
				{"title": " the THING!! ", "year": 1982}
				""", 409);

		assertThat(JsonPath.<String>read(error, "$.message")).isEqualTo("That film is already picked")
			.doesNotContain("Jas");
		assertThat(JsonPath.<List<Object>>read(state(sam), "$.picks")).hasSize(1);
	}

	@Test
	void vetoesAPickOnceTwoPeopleHaveSeenIt() throws Exception {
		Player picker = createGroup("Seen It", "Picker");
		Player ana = join(picker.code(), "Ana");
		Player ben = join(picker.code(), "Ben");
		int film = pickManually(picker, "Well Known Film", 1999);

		// The picker cannot flag their own film.
		send(post("/api/picks/{id}/seen", film), picker, null, 409);

		// One person has seen it, tapping twice: that counts once, so the film stays.
		send(post("/api/picks/{id}/seen", film), ana, null, 204);
		send(post("/api/picks/{id}/seen", film), ana, null, 204);
		assertThat(JsonPath.<List<Integer>>read(state(ben), "$.picks[*].seenCount")).containsExactly(1);
		assertThat(JsonPath.<List<Integer>>read(state(ana), "$.you.seenPickIds")).containsExactly(film);
		assertThat(JsonPath.<List<Integer>>read(state(ben), "$.you.seenPickIds")).isEmpty();

		// Ana takes it back, then flags again: still one.
		send(delete("/api/picks/{id}/seen", film), ana, null, 204);
		assertThat(JsonPath.<List<Integer>>read(state(ben), "$.picks[*].seenCount")).containsExactly(0);
		send(post("/api/picks/{id}/seen", film), ana, null, 204);
		assertThat(JsonPath.<Boolean>read(state(picker), "$.you.pickVetoed")).isFalse();

		// A second person has seen it: the pick is removed.
		send(post("/api/picks/{id}/seen", film), ben, null, 204);
		assertThat(JsonPath.<List<Object>>read(state(ben), "$.picks")).isEmpty();

		// Only the picker is told, and they cannot simply pick the same film again.
		assertThat(JsonPath.<Boolean>read(state(picker), "$.you.pickVetoed")).isTrue();
		assertThat(JsonPath.<Boolean>read(state(ana), "$.you.pickVetoed")).isFalse();
		send(post("/api/groups/{code}/picks", picker.code()), picker, """
				{"title": "Well Known Film", "year": 1999}
				""", 409);
		send(post("/api/groups/{code}/start", picker.code()), picker, null, 409);

		pickManually(picker, "Obscure Film", 2003);
		assertThat(JsonPath.<Boolean>read(state(picker), "$.you.pickVetoed")).isFalse();
	}

	@Test
	void rejoiningGivesANewSessionAndEndsTheOldOne() throws Exception {
		Player oldPhone = createGroup("Rejoin", "Jas");
		String rejoinCode = JsonPath.read(state(oldPhone), "$.you.rejoinCode");

		send(post("/api/groups/{code}/rejoin", oldPhone.code()), null, """
				{"rejoinCode": "WRONG1"}
				""", 404);

		// Typed in lower case on the new phone; still accepted.
		String json = send(post("/api/groups/{code}/rejoin", oldPhone.code()), null, """
				{"rejoinCode": "%s"}
				""".formatted(rejoinCode.toLowerCase()), 200);
		Player newPhone = new Player(oldPhone.code(), JsonPath.read(json, "$.token"));

		assertThat(newPhone.token()).isNotEqualTo(oldPhone.token());
		send(get("/api/groups/{code}", oldPhone.code()), oldPhone, null, 401);
		String state = state(newPhone);
		assertThat(JsonPath.<String>read(state, "$.you.nickname")).isEqualTo("Jas");
		assertThat(JsonPath.<Boolean>read(state, "$.you.host")).isTrue();
		assertThat(JsonPath.<String>read(state, "$.you.rejoinCode")).isEqualTo(rejoinCode);
	}

	@Test
	void onlyTheHostCanRemovePeopleAndLeavingTheLobbyTakesThePickAlong() throws Exception {
		Player jas = createGroup("Lobby", "Jas");
		Player sam = join(jas.code(), "Sam");
		Player ria = join(jas.code(), "Ria");
		pickManually(sam, "Sam's Film", 2001);
		assertThat(JsonPath.<String>read(state(sam), "$.host")).isEqualTo("Jas");
		assertThat(JsonPath.<Boolean>read(state(sam), "$.you.host")).isFalse();

		send(post("/api/groups/{code}/kick", jas.code()), sam, """
				{"nickname": "Ria"}
				""", 403);
		send(post("/api/groups/{code}/kick", jas.code()), jas, """
				{"nickname": "ria"}
				""", 204);

		// The removed member's session no longer works.
		send(get("/api/groups/{code}", jas.code()), ria, null, 401);
		assertThat(JsonPath.<List<String>>read(state(jas), "$.members")).containsExactly("Jas", "Sam");

		send(post("/api/groups/{code}/leave", jas.code()), sam, null, 204);
		String state = state(jas);
		assertThat(JsonPath.<List<String>>read(state, "$.members")).containsExactly("Jas");
		assertThat(JsonPath.<List<Object>>read(state, "$.picks")).isEmpty();
	}

	@Test
	void whenTheHostLeavesTheNextMemberTakesOverAndAnEmptyGroupIsDeleted() throws Exception {
		Player jas = createGroup("Handover", "Jas");
		Player sam = join(jas.code(), "Sam");

		send(post("/api/groups/{code}/leave", jas.code()), jas, null, 204);
		String state = state(sam);
		assertThat(JsonPath.<String>read(state, "$.host")).isEqualTo("Sam");
		assertThat(JsonPath.<Boolean>read(state, "$.you.host")).isTrue();

		send(post("/api/groups/{code}/leave", sam.code()), sam, null, 204);
		send(post("/api/groups/{code}/members", sam.code()), null, """
				{"nickname": "Late"}
				""", 404);
	}

	@Test
	void removingSomeoneMidMarathonKeepsTheirFilmAndLetsTheRestFinish() throws Exception {
		Player jas = createGroup("Asleep", "Jas");
		Player sam = join(jas.code(), "Sam");
		Player ria = join(jas.code(), "Ria");
		int filmA = pickManually(jas, "Film A", 2001);
		int filmB = pickManually(sam, "Film B", 2002);
		int filmC = pickManually(ria, "Film C", 2003);
		send(post("/api/groups/{code}/start", jas.code()), jas, null, 200);

		// Jas and Sam rate everything they can. Ria rates one film, then falls asleep.
		rate(jas, filmB, 5, 5, 5, 5, 5);
		rate(jas, filmC, 8, 8, 8, 8, 8);
		rate(sam, filmA, 6, 6, 6, 6, 6);
		rate(sam, filmC, 8, 8, 8, 8, 8);
		rate(ria, filmA, 1, 1, 1, 1, 1);
		assertThat(JsonPath.<String>read(state(jas), "$.status")).isEqualTo("WATCHING");

		send(post("/api/groups/{code}/kick", jas.code()), jas, """
				{"nickname": "Ria"}
				""", 204);

		// Ria's film is still in the marathon, and with her gone nothing is left to rate.
		String state = state(jas);
		assertThat(JsonPath.<String>read(state, "$.status")).isEqualTo("TICKETS");
		assertThat(JsonPath.<List<String>>read(state, "$.members")).containsExactly("Jas", "Sam");

		// Two people can only give their tickets to each other: everyone is tied, so no boost.
		giveTicket(jas, "Sam");
		giveTicket(sam, "Jas");
		assertThat(JsonPath.<String>read(state(jas), "$.status")).isEqualTo("DONE");

		String results = send(get("/api/groups/{code}/results", jas.code()), sam, null, 200);
		// Film C keeps both ratings; Film A loses Ria's 1s along with Ria.
		assertThat(JsonPath.<List<String>>read(results, "$.films[*].title")).containsExactly("Film C", "Film A",
				"Film B");
		assertThat(JsonPath.<List<Double>>read(results, "$.films[*].score")).containsExactly(8.0, 6.0, 5.0);
		assertThat(JsonPath.<List<Integer>>read(results, "$.films[*].raterCount")).containsExactly(2, 1, 1);
		assertThat(JsonPath.<List<String>>read(results, "$.goldenTicket.winners")).isEmpty();
		assertThat(JsonPath.<List<Double>>read(results, "$.films[*].ticketBoost")).containsExactly(0.0, 0.0, 0.0);
	}

	@Test
	void rejectsATokenFromAnotherGroup() throws Exception {
		Player jas = createGroup("Mine", "Jas");
		Player stranger = createGroup("Theirs", "Stranger");

		send(get("/api/groups/{code}", jas.code()), stranger, null, 403);
	}

	private record Player(String code, String token) {
	}

	private Player createGroup(String name, String nickname) throws Exception {
		String json = send(post("/api/groups"), null, """
				{"name": "%s", "nickname": "%s"}
				""".formatted(name, nickname), 201);
		return new Player(JsonPath.read(json, "$.code"), JsonPath.read(json, "$.token"));
	}

	private Player join(String code, String nickname) throws Exception {
		String json = send(post("/api/groups/{code}/members", code), null, """
				{"nickname": "%s"}
				""".formatted(nickname), 201);
		return new Player(code, JsonPath.read(json, "$.token"));
	}

	private int pickManually(Player player, String title, int year) throws Exception {
		String json = send(post("/api/groups/{code}/picks", player.code()), player, """
				{"title": "%s", "year": %d}
				""".formatted(title, year), 200);
		return JsonPath.read(json, "$.id");
	}

	private void rate(Player player, int pickId, int scariness, int atmosphere, int story, int acting, int enjoyment)
			throws Exception {
		rateExpecting(200, player, pickId, scariness, atmosphere, story, acting, enjoyment);
	}

	private void rateExpecting(int expectedStatus, Player player, int pickId, int scariness, int atmosphere, int story,
			int acting, int enjoyment) throws Exception {
		send(put("/api/picks/{id}/rating", pickId), player, """
				{"scariness": %d, "atmosphere": %d, "story": %d, "acting": %d, "enjoyment": %d}
				""".formatted(scariness, atmosphere, story, acting, enjoyment), expectedStatus);
	}

	private void giveTicket(Player player, String nickname) throws Exception {
		giveTicketExpecting(204, player, nickname);
	}

	private void giveTicketExpecting(int expectedStatus, Player player, String nickname) throws Exception {
		send(put("/api/groups/{code}/ticket", player.code()), player, """
				{"nickname": "%s"}
				""".formatted(nickname), expectedStatus);
	}

	private String state(Player player) throws Exception {
		return send(get("/api/groups/{code}", player.code()), player, null, 200);
	}

	// Sends one request as the given player (or anonymously), checks the status and returns the body.
	private String send(MockHttpServletRequestBuilder request, Player player, String body, int expectedStatus)
			throws Exception {
		if (player != null) {
			request.header("X-Member-Token", player.token());
		}
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
	}

	private static void assertPicksAreAnonymous(String stateJson, String... nicknames) {
		List<Map<String, Object>> picks = JsonPath.read(stateJson, "$.picks");
		assertThat(picks).isNotEmpty();
		for (Map<String, Object> pick : picks) {
			assertThat(pick.keySet()).isEqualTo(PICK_FIELDS);
			assertThat(pick.values()).doesNotContain((Object[]) nicknames);
		}
	}

}
