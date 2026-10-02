package io.github.jasjotgill.horror_ranker.service;

import io.github.jasjotgill.horror_ranker.domain.GoldenTicket;
import io.github.jasjotgill.horror_ranker.domain.GroupStatus;
import io.github.jasjotgill.horror_ranker.domain.Member;
import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import io.github.jasjotgill.horror_ranker.exception.ApiException;
import io.github.jasjotgill.horror_ranker.repository.GoldenTicketRepository;
import io.github.jasjotgill.horror_ranker.repository.MemberRepository;
import io.github.jasjotgill.horror_ranker.repository.MovieGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

	private final MovieGroupRepository groups;

	private final MemberRepository members;

	private final GoldenTicketRepository tickets;

	private final RatingService ratingService;

	public TicketService(MovieGroupRepository groups, MemberRepository members, GoldenTicketRepository tickets,
			RatingService ratingService) {
		this.groups = groups;
		this.members = members;
		this.tickets = tickets;
		this.ratingService = ratingService;
	}

	// Gives the caller's one golden ticket to another member, or moves it if already given.
	// It can be changed until the last member has given theirs, which ends the marathon.
	@Transactional
	public void give(Member caller, String nickname) {
		MovieGroup group = groups.findWithLockById(caller.getGroup().getId())
			.orElseThrow(() -> ApiException.notFound("No group with that code"));
		if (group.getStatus() != GroupStatus.TICKETS) {
			throw ApiException.conflict(group.getStatus() == GroupStatus.DONE ? "The marathon is over"
					: "Golden tickets are given out once rating is finished");
		}
		Member recipient = members.findByGroupIdAndNicknameIgnoreCase(group.getId(), nickname.strip())
			.orElseThrow(() -> ApiException.notFound("Nobody here has that nickname"));
		if (recipient.getId().equals(caller.getId())) {
			throw ApiException.conflict("You cannot give the golden ticket to yourself");
		}
		GoldenTicket ticket = tickets.findByGiverId(caller.getId()).orElse(null);
		if (ticket == null) {
			ticket = new GoldenTicket(members.getReferenceById(caller.getId()), recipient);
		}
		else {
			ticket.setRecipient(recipient);
		}
		// saveAndFlush so the count in advance() includes this ticket.
		tickets.saveAndFlush(ticket);
		ratingService.advance(group);
	}

}
