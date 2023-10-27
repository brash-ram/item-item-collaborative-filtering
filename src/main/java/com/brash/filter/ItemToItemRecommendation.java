package com.brash.filter;

import com.brash.data.entity.Mark;

import java.util.List;

public interface ItemToItemRecommendation {
    List<Mark> generateAllRecommendation(List<PartSimilarItems> partSimilarItems);
}
