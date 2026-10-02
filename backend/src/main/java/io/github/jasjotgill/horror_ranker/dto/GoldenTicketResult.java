package io.github.jasjotgill.horror_ranker.dto;

import java.util.List;

// Who won the golden ticket and what it was worth to each winner's film.
// winners is empty and boost is 0 when everyone tied (or nobody gave a ticket): no film is boosted.
public record GoldenTicketResult(List<String> winners, double boost) {
}
