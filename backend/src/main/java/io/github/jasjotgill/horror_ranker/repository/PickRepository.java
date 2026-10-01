package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.Pick;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickRepository extends JpaRepository<Pick, Long> {

	// Live (non-vetoed) picks of a group.
	List<Pick> findByGroupIdAndVetoedFalse(Long groupId);

	// A member's live pick; at most one, guaranteed by the partial unique index.
	Optional<Pick> findByGroupIdAndMemberIdAndVetoedFalse(Long groupId, Long memberId);

}
