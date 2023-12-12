package com.brash.filter.data;

import com.brash.data.entity.User;

import java.util.List;
import java.util.Map;

public record UserNeighbours(
        Map<User, List<SimpleSimilarUsers>> neighbours
) {
}
