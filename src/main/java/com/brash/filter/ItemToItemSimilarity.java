package com.brash.filter;

import com.brash.data.entity.Item;

public interface ItemToItemSimilarity {

    void updateAllSimilarity();
    void updateSimilarity(Item item);
}
