package com.brash.service;

import com.brash.data.entity.Mark;
import com.brash.exception.ItemNotFound;
import com.brash.exception.NoAvailableMarkException;
import com.brash.exception.UserNotFound;

import java.util.List;

public interface MarkService {

    Mark addMark(double mark, long userId, long itemId) throws UserNotFound, ItemNotFound;

    Mark getMark(long userId, long itemId) throws NoAvailableMarkException, UserNotFound, ItemNotFound;
    List<Mark> getGeneratedMarks(long userId) throws UserNotFound;
    List<Mark> getGeneratedMarks(long userId, int offset, int limit) throws UserNotFound;
    List<Mark> getMarks(long userId, int offset, int limit) throws UserNotFound;
//    List<Mark> getMarks(long itemId, int offset, int limit);
}
