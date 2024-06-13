package com.brash.service.impl;

import com.brash.data.entity.Item;
import com.brash.data.entity.Mark;
import com.brash.data.entity.User;
import com.brash.data.jpa.ItemRepository;
import com.brash.data.jpa.MarkRepository;
import com.brash.data.jpa.UserRepository;
import com.brash.exception.ItemNotFound;
import com.brash.exception.NoAvailableMarkException;
import com.brash.exception.UserNotFound;
import com.brash.service.MarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarkServiceImpl implements MarkService {

    private final MarkRepository markRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Mark addMark(double mark, long userId, long itemId) throws UserNotFound, ItemNotFound, NoAvailableMarkException {
        var optionalUser = userRepository.findByOriginalId(userId).orElseThrow(() -> new UserNotFound(userId));
        var optionalItem = itemRepository.findByOriginalId(itemId).orElseThrow(() -> new ItemNotFound(itemId));
        Optional<Mark> markOptional = markRepository.findByUserEqualsAndItemEquals(optionalUser, optionalItem);
        if (markOptional.isPresent()) {
            Mark markObjet = markOptional.get();
            if (markObjet.isGenerated() || markObjet.getMark() < mark) {
                return markRepository.save(
                        new Mark()
                                .setItem(optionalItem)
                                .setUser(optionalUser)
                                .setMark(mark)
                );
            }
        } else {
            return markRepository.save(
                    new Mark()
                            .setItem(optionalItem)
                            .setUser(optionalUser)
                            .setMark(mark)
            );
        }
        throw new NoAvailableMarkException("Mark for item " + itemId + " and user " + userId + " already exist");
    }

    @Override
    @Transactional
    public Mark getMark(long userId, long itemId) throws NoAvailableMarkException, UserNotFound, ItemNotFound {
        var optionalUser = userRepository.findByOriginalId(userId).orElseThrow(() -> new UserNotFound(userId));
        var optionalItem = itemRepository.findByOriginalId(itemId).orElseThrow(() -> new ItemNotFound(itemId));
        return markRepository.findByUserEqualsAndItemEquals(optionalUser, optionalItem)
                .orElseThrow(() ->
                        new NoAvailableMarkException(
                                "Mark not found with userId = " + userId + ", itemId = " + itemId
                        )
                );
    }

    @Override
    @Transactional
    public List<Mark> getGeneratedMarks(long userOriginalId) throws UserNotFound {
        User user = userRepository.findByOriginalId(userOriginalId).orElseThrow(() -> new UserNotFound(userOriginalId));
        return markRepository.findAllByUserAndMarkGreaterThenAverage(user);
    }

    @Override
    @Transactional
    public List<Mark> getGeneratedMarks(long userOriginalId, int offset, int limit) throws UserNotFound {
        User user = userRepository.findByOriginalId(userOriginalId).orElseThrow(() -> new UserNotFound(userOriginalId));
        Page<Mark> marksGreaterThanAverageMarkValue =
                markRepository.findAllByIsGeneratedAndUser(
                        true,
                        user,
                        PageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "mark")));
        return marksGreaterThanAverageMarkValue.toList();
    }

    @Override
    @Transactional
    public List<Mark> getMarks(long userOriginalId, int offset, int limit) throws UserNotFound {
        User user = userRepository.findByOriginalId(userOriginalId).orElseThrow(() -> new UserNotFound(userOriginalId));
        Page<Mark> marks = markRepository.findAllByUser(user, PageRequest.of(offset, limit, Sort.by(Sort.Direction.ASC, "mark")));
        return marks.toList();
    }
}
