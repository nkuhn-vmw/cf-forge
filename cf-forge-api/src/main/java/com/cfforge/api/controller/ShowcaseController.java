package com.cfforge.api.controller;

import com.cfforge.api.service.ShowcaseService;
import com.cfforge.api.service.ShowcaseService.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for CF Weekly showcase.
 */
@RestController
@RequestMapping("/api/v1/showcase")
public class ShowcaseController {

    private final ShowcaseService showcaseService;

    public ShowcaseController(ShowcaseService showcaseService) {
        this.showcaseService = showcaseService;
    }

    @GetMapping("/candidates")
    public List<ShowcaseCandidate> getCandidates(
            @RequestParam(defaultValue = "10") int limit) {
        return showcaseService.getShowcaseCandidates(limit);
    }

    @GetMapping("/weekly")
    public ShowcaseSummary getWeeklySummary() {
        return showcaseService.generateWeeklySummary();
    }
}
