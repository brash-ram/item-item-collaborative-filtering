package com.brash.service;

import com.brash.filter.Filter;
import com.brash.filter.SimilarityStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilterScheduler {

    private final Filter filter;
    private final SimilarityStorage similarityStorage;

    @Async
    @Scheduled(fixedDelayString = "${times.time-update-filter}")
    public void updateRecommendations() {
        filter.updateRecommendations();
    }

    @Async
    @Scheduled(fixedDelayString = "${times.time-update-similarity}")
    public void updateSimilarity() {
        filter.generateItemAndUserSimilarity();
        log.error(similarityStorage.getUserNeighbours().toString());
        log.error(similarityStorage.getItemNeighbours().toString());
    }
}
