package com.brash.filter.data;

import com.brash.data.entity.User;

import java.util.List;
import java.util.Map;

/**
 * Ближайшие соседи элемента
 * @param neighbours Структура, где ключ это пользователь, а значение список его ближайших соседей
 */
public record UserNeighbours(
        Map<User, List<SimpleSimilarUsers>> neighbours
) {
}
