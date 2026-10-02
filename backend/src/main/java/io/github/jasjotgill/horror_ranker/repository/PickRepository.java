package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.Pick;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickRepository extends JpaRepository<Pick, Long> {

	// Every pick of a group, vetoed ones included.
	List<Pick> findByGroupId(Long groupId);

	// Live (non-vetoed) picks of a group.
	List<Pick> findByGroupIdAndVetoedFalse(Long groupId);

	// A member's live pick; at most one, guaranteed by the partial unique index.
	Optional<Pick> findByGroupIdAndMemberIdAndVetoedFalse(Long groupId, Long memberId);

	// Every pick a member has made, vetoed ones included.
	List<Pick> findByMemberId(Long memberId);

	long countByGroupIdAndVetoedFalse(Long groupId);

	// Live picks whose picker is still in the group.
	long countByGroupIdAndVetoedFalseAndMemberIsNotNull(Long groupId);

	// A member's most recent pick, live or vetoed.
	Optional<Pick> findFirstByMemberIdOrderByIdDesc(Long memberId);

}
