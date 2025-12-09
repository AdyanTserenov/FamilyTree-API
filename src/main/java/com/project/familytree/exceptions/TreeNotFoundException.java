package com.project.familytree.exceptions;

public class TreeNotFoundException extends RuntimeException {
    public TreeNotFoundException(String message) {
        super(message);
    }
}
