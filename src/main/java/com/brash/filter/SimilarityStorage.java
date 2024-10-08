package com.brash.filter;

import com.brash.data.entity.Item;
import com.brash.filter.data.ItemNeighbours;
import com.brash.filter.data.SimpleSimilarItems;
import com.brash.filter.data.UserNeighbours;
import com.brash.util.FilterUtils;
import com.brash.util.PageableUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
public class SimilarityStorage {

    private ItemNeighbours itemNeighbours;

    private UserNeighbours userNeighbours;

    private boolean isGenerating;

    public List<SimpleSimilarItems> getNeighbours(Item item) {
        return itemNeighbours.neighbours().get(item);
    }

    public List<SimpleSimilarItems> getNeighbours(Item item, int offset, int limit) {
        if (itemNeighbours != null) {
            List<SimpleSimilarItems> similarItems = itemNeighbours.neighbours().get(item);
            FilterUtils.getSortedListSimilarItems(similarItems);
            return PageableUtils.getPage(similarItems, offset, limit);
        }
        return new ArrayList<>();
    }
}
