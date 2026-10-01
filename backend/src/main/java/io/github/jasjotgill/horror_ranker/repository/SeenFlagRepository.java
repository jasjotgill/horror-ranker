package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.SeenFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeenFlagRepository extends JpaRepository<SeenFlag, Long> {

	Optional<SeenFlag> findByPickIdAndMemberId(Long pickId, Long memberId);

	long countByPickId(Long pickId);

	// "PickGroupId" walks flag -> pick -> group -> id.
	List<SeenFlag> findByPickGroupId(Long groupId);

}
