package com.brash.service;

import com.brash.data.entity.Mark;

import java.util.List;

public interface MarkService {

    Mark addMark(int mark, long userId, long itemId);

    List<Mark> getAllMarks();
}
