package io.github.jasjotgill.horror_ranker.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.domain.Pick;
import io.github.jasjotgill.horror_ranker.dto.GroupStateResponse;
import io.github.jasjotgill.horror_ranker.dto.MembershipResponse;
import io.github.jasjotgill.horror_ranker.dto.PickResponse;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import io.github.jasjotgill.horror_ranker.repository.PickRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {

	// Watch order once the group has started; before that, by title, so the list
	// gives no hint about who picked when.
	private static final Comparator<Pick> DISPLAY_ORDER = Comparator
		.comparing(Pick::getWatchOrder, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing(Pick::getTitle, String.CASE_INSENSITIVE_ORDER);

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final PickRepository picks;

	private final SecretGenerator secrets;

	public GroupService(MovieGroupRepository groups, MemberRepository members, PickRepository picks,
			SecretGenerator secrets) {
		this.groups = groups;
		this.members = members;
		this.picks = picks;
		this.secrets = secrets;
	}

	// One transaction: the group and its first member are saved together or not at all.
	@Transactional
	public MembershipResponse createGroup(String name, String nickname) {
		MovieGroup group = groups.save(new MovieGroup(name.strip(), newUniqueJoinCode()));
		Member creator = members.save(new Member(group, nickname.strip(), secrets.newToken()));
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
		Member member = members.save(new Member(group, trimmed, secrets.newToken()));
		return MembershipResponse.from(member);
	}

	@Transactional(readOnly = true)
	public GroupStateResponse getState(String code) {
		MovieGroup group = findGroup(code);
		List<String> nicknames = members.findByGroupIdOrderByIdAsc(group.getId())
			.stream()
			.map(Member::getNickname)
			.toList();
		List<PickResponse> livePicks = picks.findByGroupIdAndVetoedFalse(group.getId())
			.stream()
			.sorted(DISPLAY_ORDER)
			.map(PickResponse::from)
			.toList();
		return new GroupStateResponse(group.getJoinCode(), group.getName(), group.getStatus(), nicknames, livePicks);
	}

	private MovieGroup findGroup(String code) {
		return groups.findByJoinCode(code.strip().toUpperCase(Locale.ROOT))
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
	}

	private String newUniqueJoinCode() {
		String code = secrets.newJoinCode();
		while (groups.existsByJoinCode(code)) {
			code = secrets.newJoinCode();
		}
		return code;
	}

}
