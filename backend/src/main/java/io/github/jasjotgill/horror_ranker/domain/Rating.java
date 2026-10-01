package io.github.jasjotgill.horror_ranker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ratings")
public class Rating {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pick_id", nullable = false, updatable = false)
	private Pick pick;

	// The rater.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(nullable = false)
	private int scariness;

	@Column(nullable = false)
	private int atmosphere;

	@Column(nullable = false)
	private int story;

	@Column(nullable = false)
	private int acting;

	@Column(nullable = false)
	private int enjoyment;

	protected Rating() {
	}

	public Rating(Pick pick, Member member, int scariness, int atmosphere, int story, int acting, int enjoyment) {
		this.pick = pick;
		this.member = member;
		setScores(scariness, atmosphere, story, acting, enjoyment);
	}

	// One method for all five, because a rating is always submitted or updated as a whole.
	public void setScores(int scariness, int atmosphere, int story, int acting, int enjoyment) {
		this.scariness = scariness;
		this.atmosphere = atmosphere;
		this.story = story;
		this.acting = acting;
		this.enjoyment = enjoyment;
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

	public int getScariness() {
		return scariness;
	}

	public int getAtmosphere() {
		return atmosphere;
	}

	public int getStory() {
		return story;
	}

	public int getActing() {
		return acting;
	}

	public int getEnjoyment() {
		return enjoyment;
	}

}
