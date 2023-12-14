package com.brash.filter.data;

import com.brash.data.entity.Item;

import java.util.List;
import java.util.Map;

/**
 * Ближайшие соседи элемента
 * @param neighbours Структура, где ключ это элемент, а значение список его ближайших соседей
 */
public record ItemNeighbours(
        Map<Item, List<SimpleSimilarItems>> neighbours
) {
}
