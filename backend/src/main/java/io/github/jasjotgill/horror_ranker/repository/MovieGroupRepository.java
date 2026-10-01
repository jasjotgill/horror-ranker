package io.github.jasjotgill.horror_ranker.repository;

import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

// Spring Data writes the implementation at startup; method names are parsed into queries.
public interface MovieGroupRepository extends JpaRepository<MovieGroup, Long> {

	Optional<MovieGroup> findByJoinCode(String joinCode);

	boolean existsByJoinCode(String joinCode);

	// SELECT ... FOR UPDATE: a second transaction asking for the same row waits until the
	// first one commits. Used by start so two simultaneous starts cannot both shuffle.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MovieGroup> findWithLockByJoinCode(String joinCode);

}
