package com.brash.filter;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ItemToItemRecommendation {
    List<Mark> generateAllRecommendation(
            List<PartSimilarItems> partSimilarItems,
            Map<User, Set<Item>> mapForMarks
    );
}
