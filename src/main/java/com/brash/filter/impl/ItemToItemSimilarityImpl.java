package com.brash.filter.impl;

import com.brash.data.entity.Item;
import com.brash.filter.ItemToItemSimilarity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemToItemSimilarityImpl implements ItemToItemSimilarity {

    @Override
    public void updateAllSimilarity() {

    }

    @Override
    public void updateSimilarity(Item item) {

    }
}
