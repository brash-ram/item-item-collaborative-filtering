package com.brash.exception;

public class ItemNotFound extends Exception {

    public ItemNotFound(Object id) {
        super("Item not found with id " + id);
    }
}
