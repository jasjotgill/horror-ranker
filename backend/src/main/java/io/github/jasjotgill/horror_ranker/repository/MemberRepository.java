package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByToken(String token);

	List<Member> findByGroupIdOrderByIdAsc(Long groupId);

	boolean existsByGroupIdAndNicknameIgnoreCase(Long groupId, String nickname);

}
