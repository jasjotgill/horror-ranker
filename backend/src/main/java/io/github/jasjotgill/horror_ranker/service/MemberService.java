package io.github.jasjotgill.horror_ranker.service;

import java.util.List;
import java.util.Locale;

import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.dto.MembershipResponse;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import io.github.jasjotgill.horror_ranker.repository.PickRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Getting back into a group, leaving it, and being removed from it.
@Service
public class MemberService {

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final PickRepository picks;

	private final RatingService ratingService;

	private final SecretGenerator secrets;

	public MemberService(MovieGroupRepository groups, MemberRepository members, PickRepository picks,
			RatingService ratingService, SecretGenerator secrets) {
		this.groups = groups;
		this.members = members;
		this.picks = picks;
		this.ratingService = ratingService;
		this.secrets = secrets;
	}

	// Logs a member in on a new browser or device. Issuing a new token makes the old one
	// unknown, so the previous session is logged out the next time it talks to the server.
	@Transactional
	public MembershipResponse rejoin(String code, String rejoinCode) {
		MovieGroup group = groups.findByJoinCode(normalize(code))
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		Member member = members.findByGroupIdAndRejoinCode(group.getId(), normalize(rejoinCode))
			.orElseThrow(() -> ApiException.notFound("That rejoin code does not match anyone in this group"));
		member.setToken(secrets.newToken());
		return MembershipResponse.from(member);
	}

	@Transactional
	public void leave(Member caller) {
		MovieGroup group = lockGroup(caller);
		// Once the results are in, leaving only logs the browser out: removing the member
		// would delete their ratings and change results everyone has already seen.
		if (group.getStatus() == GroupStatus.DONE) {
			return;
		}
		members.findById(caller.getId()).ifPresent(member -> remove(group, member));
	}

	@Transactional
	public void kick(Member caller, String nickname) {
		MovieGroup group = lockGroup(caller);
		// Read the caller again inside this transaction: the host may have changed.
		boolean callerIsHost = members.findById(caller.getId()).map(Member::isHost).orElse(false);
		if (!callerIsHost) {
			throw ApiException.forbidden("Only the host can remove people");
		}
		if (group.getStatus() == GroupStatus.DONE) {
			throw ApiException.conflict("The marathon is over");
		}
		Member target = members.findByGroupIdAndNicknameIgnoreCase(group.getId(), nickname.strip())
			.orElseThrow(() -> ApiException.notFound("Nobody here has that nickname"));
		if (target.getId().equals(caller.getId())) {
			throw ApiException.conflict("Use Leave to remove yourself");
		}
		remove(group, target);
	}

	// In the lobby the member's pick goes with them. After the start their film stays in the
	// marathon (it is in the watch order and may already have been watched), but their ratings go.
	private void remove(MovieGroup group, Member member) {
		if (group.getStatus() == GroupStatus.LOBBY) {
			picks.deleteAll(picks.findByMemberId(member.getId()));
			picks.flush();
		}
		boolean wasHost = member.isHost();
		members.delete(member);
		// Flush now: Hibernate would otherwise run the UPDATE that names a new host before
		// this DELETE, and briefly have two hosts, which the database forbids.
		members.flush();

		List<Member> remaining = members.findByGroupIdOrderByIdAsc(group.getId());
		if (remaining.isEmpty()) {
			// Nobody left: the group and everything under it is deleted.
			groups.delete(group);
			return;
		}
		if (wasHost) {
			// Whoever joined earliest takes over.
			remaining.get(0).setHost(true);
		}
		// The person who left may have been the only one still to rate or to give a ticket.
		ratingService.advance(group);
	}

	// Locked so that leaving, removing, starting and rating in the same group happen one at a time.
	private MovieGroup lockGroup(Member caller) {
		return groups.findWithLockById(caller.getGroup().getId())
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
	}

	private static String normalize(String code) {
		return code.strip().toUpperCase(Locale.ROOT);
	}

}
