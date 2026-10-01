package io.github.jasjotgill.horror_ranker.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Not called "Group" because GROUP is a reserved word in JPQL queries.
@Entity
@Table(name = "groups")
public class MovieGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(name = "join_code", nullable = false, unique = true)
	private String joinCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GroupStatus status = GroupStatus.LOBBY;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	// JPA needs a no-argument constructor to build objects from rows.
	protected MovieGroup() {
	}

	public MovieGroup(String name, String joinCode) {
		this.name = name;
		this.joinCode = joinCode;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getJoinCode() {
		return joinCode;
	}

	public GroupStatus getStatus() {
		return status;
	}

	public void setStatus(GroupStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
