package com.brash.filter;

import com.brash.data.entity.Item;
import com.brash.filter.data.ItemNeighbours;
import com.brash.filter.data.SimpleSimilarItems;
import com.brash.filter.data.UserNeighbours;
import com.brash.util.PageableUtils;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SimilarityStorage {

    @Getter
    private ItemNeighbours itemNeighbours;

    @Getter
    private UserNeighbours userNeighbours;

    private LocalDateTime lastUpdate;

    public List<SimpleSimilarItems> getNeighbours(Item item) {
        return itemNeighbours.neighbours().get(item);
    }

    public List<SimpleSimilarItems> getNeighbours(Item item, int offset, int limit) {
        List<SimpleSimilarItems> similarItems = itemNeighbours.neighbours().get(item);
        return PageableUtils.getPage(similarItems, offset, limit);
    }

    public void setItemNeighbours(ItemNeighbours itemNeighbours) {
        this.itemNeighbours = itemNeighbours;
        lastUpdate = LocalDateTime.now();
    }

    public void setUserNeighbours(UserNeighbours userNeighbours) {
        this.userNeighbours = userNeighbours;
        lastUpdate = LocalDateTime.now();
    }
}
