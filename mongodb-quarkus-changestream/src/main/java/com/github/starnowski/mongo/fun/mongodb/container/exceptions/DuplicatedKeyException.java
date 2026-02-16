package com.github.starnowski.mongo.fun.mongodb.container.exceptions;

public class DuplicatedKeyException extends RuntimeException{

    public DuplicatedKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
