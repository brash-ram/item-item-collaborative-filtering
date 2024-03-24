package com.brash.service.impl;

import com.brash.data.entity.Item;
import com.brash.data.jpa.ItemRepository;
import com.brash.dto.web.ItemsDTO;
import com.brash.exception.ItemNotFound;
import com.brash.filter.SimilarityStorage;
import com.brash.filter.data.SimpleSimilarItems;
import com.brash.service.ItemService;
import com.brash.util.FilterUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    private final SimilarityStorage similarityStorage;

    @Override
    public Item addItem(long originalId) {
        return itemRepository.save(new Item().setOriginalId(originalId));
    }

    @Override
    public ItemsDTO getSimilarity(long itemId, int offset, int limit) throws ItemNotFound {
        Item item = itemRepository.findByOriginalId(itemId).orElseThrow(() -> new ItemNotFound(itemId));
        List<SimpleSimilarItems> similarItems = similarityStorage.getNeighbours(item, offset, limit);
        return new ItemsDTO(
                similarItems.stream()
                .map(items -> FilterUtils.getOtherItem(items, item).getOriginalId())
                .toList()
        );
    }

    @Override
    public void remove(long itemId) {
        itemRepository.deleteById(itemId);
    }


}
