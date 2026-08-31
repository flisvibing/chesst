package com.chesst.opening;

import com.chesst.opening.dto.OpeningResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/openings")
public class OpeningController {

    private final OpeningService openingService;

    public OpeningController(OpeningService openingService) {
        this.openingService = openingService;
    }

    @GetMapping
    public Page<OpeningResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String eco,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size
    ) {
        return openingService.search(q, eco, page, size);
    }

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        return openingService.volumeCounts();
    }

    @GetMapping("/{id}")
    public OpeningResponse get(@PathVariable Long id) {
        return openingService.get(id);
    }
}
