package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

	Optional<Rating> findByPickIdAndMemberId(Long pickId, Long memberId);

	// "PickGroupId" walks rating -> pick -> group -> id.
	List<Rating> findByPickGroupId(Long groupId);

}
