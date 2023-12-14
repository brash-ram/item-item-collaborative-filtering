package com.brash.filter.data;

import com.brash.data.entity.Mark;

public record SimilarUser(
        SimpleSimilarUsers similarUsers,
        Mark mark
) {
}
