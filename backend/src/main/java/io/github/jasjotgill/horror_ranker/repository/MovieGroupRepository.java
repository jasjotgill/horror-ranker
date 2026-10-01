package io.github.jasjotgill.horror_ranker.repository;

import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.MovieGroup;
import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data writes the implementation at startup; method names are parsed into queries.
public interface MovieGroupRepository extends JpaRepository<MovieGroup, Long> {

	Optional<MovieGroup> findByJoinCode(String joinCode);

	boolean existsByJoinCode(String joinCode);

}
