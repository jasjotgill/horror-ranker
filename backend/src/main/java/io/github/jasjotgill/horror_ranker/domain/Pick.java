package io.github.jasjotgill.horror_ranker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "picks")
public class Pick {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "group_id", nullable = false, updatable = false)
	private MovieGroup group;

	// The picker. The server needs it; no DTO may ever expose it.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private PickSource source;

	@Column(name = "tmdb_id", updatable = false)
	private Long tmdbId;

	@Column(nullable = false)
	private String title;

	@Column(name = "release_year", nullable = false)
	private Integer releaseYear;

	@Column(name = "poster_url")
	private String posterUrl;

	// Null until the group starts.
	@Column(name = "watch_order")
	private Integer watchOrder;

	@Column(nullable = false)
	private boolean vetoed = false;

	protected Pick() {
	}

	private Pick(MovieGroup group, Member member, PickSource source, Long tmdbId, String title, Integer releaseYear,
			String posterUrl) {
		this.group = group;
		this.member = member;
		this.source = source;
		this.tmdbId = tmdbId;
		this.title = title;
		this.releaseYear = releaseYear;
		this.posterUrl = posterUrl;
	}

	// Two factory methods instead of a public constructor, so a pick can only be built
	// in the two shapes the database CHECK allows.
	public static Pick fromTmdb(MovieGroup group, Member member, long tmdbId, String title, Integer releaseYear,
			String posterUrl) {
		return new Pick(group, member, PickSource.TMDB, tmdbId, title, releaseYear, posterUrl);
	}

	public static Pick manual(MovieGroup group, Member member, String title, Integer releaseYear, String posterUrl) {
		return new Pick(group, member, PickSource.MANUAL, null, title, releaseYear, posterUrl);
	}

	public Long getId() {
		return id;
	}

	public MovieGroup getGroup() {
		return group;
	}

	public Member getMember() {
		return member;
	}

	public PickSource getSource() {
		return source;
	}

	public Long getTmdbId() {
		return tmdbId;
	}

	public String getTitle() {
		return title;
	}

	public Integer getReleaseYear() {
		return releaseYear;
	}

	public String getPosterUrl() {
		return posterUrl;
	}

	public Integer getWatchOrder() {
		return watchOrder;
	}

	public void setWatchOrder(Integer watchOrder) {
		this.watchOrder = watchOrder;
	}

	public boolean isVetoed() {
		return vetoed;
	}

	public void setVetoed(boolean vetoed) {
		this.vetoed = vetoed;
	}

}
