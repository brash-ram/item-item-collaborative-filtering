package com.brash.service;

import com.brash.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilterScheduler {

    private final Filter filter;

    @Async
    @Scheduled(fixedDelayString = "${time-update-filter}")
    public void updateRecommendations() {
        filter.updateRecommendations();
    }

    @Async
    @Scheduled(fixedDelayString = "${time-update-similarity}")
    public void updateSimilarity() {
        filter.generateItemAndUserSimilarity();
    }
}
