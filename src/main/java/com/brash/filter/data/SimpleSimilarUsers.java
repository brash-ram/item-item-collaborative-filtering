package com.brash.filter.data;

import com.brash.data.entity.User;

/**
 * Простая пара сходства 2 пользователей
 * @param user1 Первый пользователь
 * @param user2 Второй пользователь
 * @param similarValue Значение их сходства
 */
public record SimpleSimilarUsers(
        User user1,
        User user2,
        double similarValue
) {
}
