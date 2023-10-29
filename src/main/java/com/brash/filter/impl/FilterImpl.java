package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.filter.Filter;
import com.brash.filter.ItemToItemRecommendation;
import com.brash.filter.ItemToItemSimilarity;
import com.brash.filter.PartSimilarItems;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilterImpl implements Filter {

    private final ItemToItemSimilarity itemToItemSimilarity;

    private final ItemToItemRecommendation itemToItemRecommendation;

    private final ExecutorService executorService;

    private final ItemRepository itemRepository;
    private final MarkRepository markRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void updateRecommendations() {
        List<Item> items = itemRepository.findAll();
        List<User> users = userRepository.findAll();
        List<PartSimilarItems> parts = itemToItemSimilarity.updateSimilarity(items);
        Map<User, Set<Item>> mapForMarks = getUserAndItemForRecommendationMark(new HashSet<>(items), users);
        List<Mark> generatedMarks = itemToItemRecommendation.generateAllRecommendation(parts, mapForMarks);
        markRepository.saveAll(generatedMarks);
    }

    protected Map<User, Set<Item>> getUserAndItemForRecommendationMark(Set<Item> items, List<User> users) {
        Map<User, Set<Item>> mapForMark = new HashMap<>();

        for (User user : users) {
            try {
                Set<Item> allUserItems = user.getMarks().stream()
                        .map(Mark::getItem).collect(Collectors.toSet());
                Set<Item> itemsWithoutUserMark = Sets.difference(items, allUserItems);
                Set<Item> userItemsWithGeneratedMark = user.getMarks().stream()
                        .filter(Mark::getIsGenerated)
                        .map(Mark::getItem).collect(Collectors.toSet());
                userItemsWithGeneratedMark.addAll(itemsWithoutUserMark);
                mapForMark.put(user, userItemsWithGeneratedMark);
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }
        return mapForMark;
    }
}
