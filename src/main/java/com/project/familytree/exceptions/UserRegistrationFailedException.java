package com.project.familytree.exceptions;

public class UserRegistrationFailedException extends RuntimeException {
    public UserRegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
