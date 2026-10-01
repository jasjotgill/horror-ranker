package io.github.jasjotgill.horror_ranker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One member saying "I've seen it" about one pick.
@Entity
@Table(name = "seen_flags")
public class SeenFlag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pick_id", nullable = false, updatable = false)
	private Pick pick;

	// Who flagged it. Needed to count each person once; never sent to a client.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	protected SeenFlag() {
	}

	public SeenFlag(Pick pick, Member member) {
		this.pick = pick;
		this.member = member;
	}

	public Long getId() {
		return id;
	}

	public Pick getPick() {
		return pick;
	}

	public Member getMember() {
		return member;
	}

}
