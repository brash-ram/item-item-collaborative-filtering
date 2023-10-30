package com.brash.service;

import com.brash.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilterScheduler {

    private final Filter filter;

    @Scheduled(cron = "0 0 0 * * ?")
    public void updateRecommendations() {
        filter.updateRecommendations();
    }
}
