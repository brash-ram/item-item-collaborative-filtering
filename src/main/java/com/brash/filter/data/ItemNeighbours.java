package com.brash.filter.data;

import com.brash.data.entity.Item;

import java.util.List;
import java.util.Map;

public record ItemNeighbours(
        Map<Item, List<SimpleSimilarItems>> neighbours
) {
}
