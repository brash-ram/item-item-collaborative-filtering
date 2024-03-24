package com.brash.exception;

public class UserNotFound extends Exception {

    public UserNotFound(Object id) {
        super("User not found with id " + id);
    }
}
