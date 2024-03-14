package com.brash.service.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.exception.NoAvailableMarkException;
import com.brash.service.MarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public Mark addMark(double mark, long userId, long itemId) {
        return markRepository.save(
                new Mark()
                        .setItem(itemRepository.findByOriginalId(itemId))
                        .setUser(userRepository.findByOriginalId(userId))
        );
    }

    @Override
    @Transactional
    public Mark getMark(long userId, long itemId) throws NoAvailableMarkException {
        User user = userRepository.findByOriginalId(userId);
        Item item = itemRepository.findByOriginalId(itemId);
        return markRepository.findByUserEqualsAndItemEquals(user, item)
                .orElseThrow(() ->
                        new NoAvailableMarkException(
                                "Mark not found with userId = " + userId + ", itemId = " + itemId
                        )
                );
    }

    @Override
    @Transactional
    public List<Mark> getGeneratedMarks(long userOriginalId) {
        User user = userRepository.findByOriginalId(userOriginalId);
        return markRepository.findAllByUserAndMarkGreaterThenAverage(user);
    }

    @Override
    @Transactional
    public List<Mark> getGeneratedMarks(long userOriginalId, int offset, int limit) {
        User user = userRepository.findByOriginalId(userOriginalId);
        Page<Mark> marksGreaterThanAverageMarkValue =
                markRepository.findAllByIsGenerated(
                        true,
                        PageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "mark_value")));
        return marksGreaterThanAverageMarkValue.toList();
    }

    @Override
    @Transactional
    public List<Mark> getMarks(long userOriginalId, int offset, int limit) {
        User user = userRepository.findByOriginalId(userOriginalId);
        Page<Mark> marks = markRepository.findAll(PageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "mark_value")));
        return marks.toList();
    }
}
