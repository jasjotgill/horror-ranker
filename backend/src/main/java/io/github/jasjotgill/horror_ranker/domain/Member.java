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

	// The login secret held by one browser. Replaced when the member rejoins elsewhere,
	// which is what logs the old browser out.
	@Column(nullable = false, unique = true)
	private String token;

	// A short code the member can type on another device to get back in as themselves.
	@Column(name = "rejoin_code", nullable = false, updatable = false)
	private String rejoinCode;

	// The host starts the marathon and can remove people. One per group.
	@Column(nullable = false)
	private boolean host;

	protected Member() {
	}

	public Member(MovieGroup group, String nickname, String token, String rejoinCode, boolean host) {
		this.group = group;
		this.nickname = nickname;
		this.token = token;
		this.rejoinCode = rejoinCode;
		this.host = host;
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

	public void setToken(String token) {
		this.token = token;
	}

	public String getRejoinCode() {
		return rejoinCode;
	}

	public boolean isHost() {
		return host;
	}

	public void setHost(boolean host) {
		this.host = host;
	}

}
