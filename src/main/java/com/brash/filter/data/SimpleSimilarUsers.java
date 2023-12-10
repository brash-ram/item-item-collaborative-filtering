package com.brash.filter.data;

import com.brash.data.entity.User;

public record SimpleSimilarUsers(
        User user1,
        User user2,
        double similarValue
) {
}
