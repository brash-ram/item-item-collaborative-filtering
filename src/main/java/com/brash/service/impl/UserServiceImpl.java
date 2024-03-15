package com.brash.service.impl;

import com.brash.data.entity.User;
import com.brash.data.jpa.UserRepository;
import com.brash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User addUser(long originalId) {
        return userRepository.save(new User().setOriginalId(originalId));
    }

    @Override
    public void remove(long userId) {
        userRepository.deleteById(userId);
    }
}
