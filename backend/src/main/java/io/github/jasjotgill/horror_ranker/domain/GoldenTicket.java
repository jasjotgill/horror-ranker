package io.github.jasjotgill.horror_ranker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One member's golden ticket, given to another member.
@Entity
@Table(name = "golden_tickets")
public class GoldenTicket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "giver_id", nullable = false, updatable = false)
	private Member giver;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private Member recipient;

	protected GoldenTicket() {
	}

	public GoldenTicket(Member giver, Member recipient) {
		this.giver = giver;
		this.recipient = recipient;
	}

	public Long getId() {
		return id;
	}

	public Member getGiver() {
		return giver;
	}

	public Member getRecipient() {
		return recipient;
	}

	public void setRecipient(Member recipient) {
		this.recipient = recipient;
	}

}
