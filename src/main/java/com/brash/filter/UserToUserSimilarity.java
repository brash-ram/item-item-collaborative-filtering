package com.brash.filter;

import com.brash.data.entity.User;
import com.brash.filter.data.SimilarItems;

import java.util.List;

public interface UserToUserSimilarity {
    List<SimilarItems> updateSimilarity(List<User> items);
}
