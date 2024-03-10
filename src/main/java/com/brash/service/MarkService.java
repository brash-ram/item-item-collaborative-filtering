package com.brash.service;

import com.brash.data.entity.Mark;
import com.brash.dto.web.MarkDTO;
import com.brash.exception.NoAvailableMarkException;

import java.util.List;

public interface MarkService {

    Mark addMark(double mark, long userId, long itemId);

    Mark getMark(long userId, long itemId) throws NoAvailableMarkException;
    List<Mark> getGeneratedMarks(long userId);
    List<Mark> getGeneratedMarks(long userId, int offset, int limit);
    List<Mark> getMarks(long userId, int offset, int limit);
//    List<Mark> getMarks(long itemId, int offset, int limit);
}
