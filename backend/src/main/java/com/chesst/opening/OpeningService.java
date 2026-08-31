package com.chesst.opening;

import com.chesst.opening.dto.OpeningResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpeningService {

    private final OpeningRepository openings;

    public OpeningService(OpeningRepository openings) {
        this.openings = openings;
    }

    @Transactional(readOnly = true)
    public Page<OpeningResponse> search(String q, String eco, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, size));
        return openings.search(q == null ? "" : q, eco == null ? "" : eco.toUpperCase(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OpeningResponse get(Long id) {
        return openings.findById(id).map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> volumeCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (char c = 'A'; c <= 'E'; c++) {
            counts.put(String.valueOf(c), openings.countByEcoStartingWith(String.valueOf(c)));
        }
        counts.put("ALL", openings.count());
        return counts;
    }

    private OpeningResponse toResponse(Opening o) {
        return new OpeningResponse(
                o.getId(), o.getEco(), o.getName(), o.getPgn(), o.getFen(),
                o.getWhiteWins(), o.getDraws(), o.getBlackWins()
        );
    }
}
