package com.brash.service;

import com.brash.data.entity.Mark;
import com.brash.dto.MarkDTO;
import com.brash.exception.NoAvailableMarkException;

import java.util.List;

public interface MarkService {

    Mark addMark(int mark, long userId, long itemId);

    Mark getMark(long userId, long itemId) throws NoAvailableMarkException;
    List<Mark> getGeneratedMarks(long userId) throws NoAvailableMarkException;
    List<MarkDTO> getGeneratedMarksDto(long userId) throws NoAvailableMarkException;
}
