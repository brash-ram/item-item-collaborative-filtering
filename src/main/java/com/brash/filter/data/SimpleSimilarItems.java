package com.brash.filter.data;

import com.brash.data.entity.Item;

public record SimpleSimilarItems(
        Item item1,
        Item item2,
        double similarValue
) {
}
