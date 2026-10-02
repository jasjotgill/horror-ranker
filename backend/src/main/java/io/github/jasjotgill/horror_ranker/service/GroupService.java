package io.github.jasjotgill.horror_ranker.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.jasjotgill.horror_ranker.domain.GoldenTicket;
import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.domain.Pick;
import io.github.jasjotgill.horror_ranker.domain.SeenFlag;
import io.github.jasjotgill.horror_ranker.dto.GroupStateResponse;
import io.github.jasjotgill.horror_ranker.dto.MembershipResponse;
import io.github.jasjotgill.horror_ranker.dto.PickResponse;
import io.github.jasjotgill.horror_ranker.dto.RatingResponse;
import io.github.jasjotgill.horror_ranker.dto.YourStatus;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.GoldenTicketRepository;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import io.github.jasjotgill.horror_ranker.repository.PickRepository;
import io.github.jasjotgill.horror_ranker.repository.RatingRepository;
import io.github.jasjotgill.horror_ranker.repository.SeenFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

	private static final int MIN_MEMBERS_TO_START = 2;

	// Watch order once the group has started; before that, by title, so the list
	// gives no hint about who picked when.
	private static final Comparator<Pick> DISPLAY_ORDER = Comparator
		.comparing(Pick::getWatchOrder, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(Pick::getTitle, String.CASE_INSENSITIVE_ORDER);

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final PickRepository picks;

	private final RatingRepository ratings;

	private final SeenFlagRepository seenFlags;

	private final GoldenTicketRepository tickets;

	private final RatingService ratingService;

	private final SecretGenerator secrets;

	public GroupService(MovieGroupRepository groups, MemberRepository members, PickRepository picks,
			RatingRepository ratings, SeenFlagRepository seenFlags, GoldenTicketRepository tickets,
			RatingService ratingService, SecretGenerator secrets) {
		this.tickets = tickets;
		this.ratingService = ratingService;
		this.groups = groups;
		this.members = members;
		this.picks = picks;
		this.ratings = ratings;
		this.seenFlags = seenFlags;
		this.secrets = secrets;
	}

	// One transaction: the group and its first member are saved together or not at all.
	@Transactional
	public MembershipResponse createGroup(String name, String nickname) {
		MovieGroup group = groups.save(new MovieGroup(name.strip(), newUniqueJoinCode()));
		// Whoever creates the group is its host.
		Member creator = members
			.save(new Member(group, nickname.strip(), secrets.newToken(), secrets.newRejoinCode(), true));
		return MembershipResponse.from(creator);
	}

	@Transactional
	public MembershipResponse joinGroup(String code, String nickname) {
		MovieGroup group = findGroup(code);
		if (group.getStatus() != GroupStatus.LOBBY) {
			throw ApiException.conflict("This marathon has already started");
		}
		String trimmed = nickname.strip();
		if (members.existsByGroupIdAndNicknameIgnoreCase(group.getId(), trimmed)) {
			throw ApiException.conflict("That nickname is already taken in this group");
		}
		Member member = members
			.save(new Member(group, trimmed, secrets.newToken(), newUniqueRejoinCode(group), false));
		return MembershipResponse.from(member);
	}

	@Transactional(readOnly = true)
	public GroupStateResponse getState(String code, Member caller) {
		return stateOf(findGroup(code), caller);
	}

	// Locks picks and fixes the watch order. The order is decided here, once, and stored;
	// reading the group afterwards never shuffles again.
	@Transactional
	public GroupStateResponse start(String code, Member caller) {
		MovieGroup group = groups.findWithLockByJoinCode(normalizeCode(code))
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() != GroupStatus.LOBBY) {
			throw ApiException.conflict("This marathon has already started");
		}
		List<Member> groupMembers = members.findByGroupIdOrderByIdAsc(group.getId());
		// Checked against rows read in this transaction, not the caller object, which is older.
		boolean callerIsHost = groupMembers.stream()
			.anyMatch(member -> member.isHost() && member.getId().equals(caller.getId()));
		if (!callerIsHost) {
			throw ApiException.forbidden("Only the host can start the marathon");
		}
		if (groupMembers.size() < MIN_MEMBERS_TO_START) {
			throw ApiException.conflict("A marathon needs at least " + MIN_MEMBERS_TO_START + " people");
		}
		List<Pick> livePicks = new ArrayList<>(picks.findByGroupIdAndVetoedFalse(group.getId()));
		Set<Long> pickers = livePicks.stream()
			.filter(pick -> pick.getMember() != null)
			.map(pick -> pick.getMember().getId())
			.collect(Collectors.toSet());
		if (!groupMembers.stream().allMatch(member -> pickers.contains(member.getId()))) {
			throw ApiException.conflict("Not everyone has picked a film yet");
		}

		Collections.shuffle(livePicks, new SecureRandom());
		for (int i = 0; i < livePicks.size(); i++) {
			livePicks.get(i).setWatchOrder(i + 1);
		}
		group.setStatus(GroupStatus.WATCHING);
		// No save calls: these are managed entities, so the changes are written at commit,
		// all together or not at all.
		return stateOf(group, caller);
	}

	// Ends the current stage early, for the night someone falls asleep: rating moves on to
	// golden tickets, and golden tickets move on to results. Normally each stage ends by
	// itself when the last rating or ticket arrives.
	@Transactional
	public GroupStateResponse finish(String code, Member caller) {
		MovieGroup group = groups.findWithLockByJoinCode(normalizeCode(code))
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() == GroupStatus.LOBBY) {
			throw ApiException.conflict("The marathon has not started yet");
		}
		if (group.getStatus() == GroupStatus.WATCHING) {
			ratingService.endRating(group);
		}
		else {
			group.setStatus(GroupStatus.DONE);
		}
		return stateOf(group, caller);
	}

	private GroupStateResponse stateOf(MovieGroup group, Member caller) {
		List<Member> groupMembers = members.findByGroupIdOrderByIdAsc(group.getId());
		List<String> nicknames = groupMembers.stream().map(Member::getNickname).toList();
		String host = groupMembers.stream().filter(Member::isHost).map(Member::getNickname).findFirst().orElse(null);
		List<SeenFlag> flags = seenFlags.findByPickGroupId(group.getId());
		// How many members have seen each pick. Only the number leaves the server.
		Map<Long, Long> seenCounts = flags.stream()
			.collect(Collectors.groupingBy(flag -> flag.getPick().getId(), Collectors.counting()));
		List<PickResponse> livePicks = picks.findByGroupIdAndVetoedFalse(group.getId())
			.stream()
			.sorted(DISPLAY_ORDER)
			.map(pick -> PickResponse.from(pick, seenCounts.getOrDefault(pick.getId(), 0L).intValue()))
			.toList();
		List<GoldenTicket> groupTickets = tickets.findByGiverGroupId(group.getId());
		String ticketFor = groupTickets.stream()
			.filter(ticket -> ticket.getGiver().getId().equals(caller.getId()))
			.map(ticket -> ticket.getRecipient().getNickname())
			.findFirst()
			.orElse(null);
		return new GroupStateResponse(group.getJoinCode(), group.getName(), group.getStatus(), host, nicknames,
				livePicks, livePicks.size(), groupTickets.size(), yourStatus(group, caller, flags, seenCounts, ticketFor));
	}

	private YourStatus yourStatus(MovieGroup group, Member caller, List<SeenFlag> flags, Map<Long, Long> seenCounts,
			String ticketFor) {
		PickResponse ownPick = picks.findByGroupIdAndMemberIdAndVetoedFalse(group.getId(), caller.getId())
			.map(pick -> PickResponse.from(pick, seenCounts.getOrDefault(pick.getId(), 0L).intValue()))
			.orElse(null);
		List<Long> seenPickIds = flags.stream()
			.filter(flag -> flag.getMember().getId().equals(caller.getId()))
			.map(flag -> flag.getPick().getId())
			.sorted()
			.toList();
		boolean vetoed = ownPick == null
				&& picks.findFirstByMemberIdOrderByIdDesc(caller.getId()).map(Pick::isVetoed).orElse(false);
		List<RatingResponse> ownRatings = ratings.findByMemberId(caller.getId())
			.stream()
			.map(RatingResponse::from)
			.sorted(Comparator.comparing(RatingResponse::pickId))
			.toList();
		return new YourStatus(caller.getNickname(), caller.isHost(), caller.getRejoinCode(), ownPick, vetoed,
				seenPickIds, ownRatings, ticketFor);
	}

	private MovieGroup findGroup(String code) {
		return groups.findByJoinCode(normalizeCode(code))
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
	}

	private static String normalizeCode(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}

	private String newUniqueRejoinCode(MovieGroup group) {
		String code = secrets.newRejoinCode();
		while (members.existsByGroupIdAndRejoinCode(group.getId(), code)) {
			code = secrets.newRejoinCode();
		}
		return code;
	}

	private String newUniqueJoinCode() {
		String code = secrets.newJoinCode();
		while (groups.existsByJoinCode(code)) {
			code = secrets.newJoinCode();
		}
		return code;
	}

}
