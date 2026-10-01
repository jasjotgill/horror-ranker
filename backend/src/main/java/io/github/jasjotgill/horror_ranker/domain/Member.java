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
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false, updatable = false)
	private MovieGroup group;

	@Column(nullable = false)
	private String nickname;

	@Column(nullable = false, unique = true, updatable = false)
	private String token;

	protected Member() {
	}

	public Member(MovieGroup group, String nickname, String token) {
		this.group = group;
		this.nickname = nickname;
		this.token = token;
	}

	public Long getId() {
		return id;
	}

	public MovieGroup getGroup() {
		return group;
	}

	public String getNickname() {
		return nickname;
	}

	public String getToken() {
		return token;
	}

}
