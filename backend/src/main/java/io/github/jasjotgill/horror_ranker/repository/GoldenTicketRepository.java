package io.github.jasjotgill.horror_ranker.repository;

import java.util.List;
import java.util.Optional;

import io.github.jasjotgill.horror_ranker.domain.GoldenTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoldenTicketRepository extends JpaRepository<GoldenTicket, Long> {

	Optional<GoldenTicket> findByGiverId(Long giverId);

	// "GiverGroupId" walks ticket -> giver -> group -> id.
	List<GoldenTicket> findByGiverGroupId(Long groupId);

	long countByGiverGroupId(Long groupId);

}
