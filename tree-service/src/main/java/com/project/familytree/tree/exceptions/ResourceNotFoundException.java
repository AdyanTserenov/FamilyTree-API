package com.project.familytree.tree.exceptions;

/**
 * Thrown when a requested resource (tree, person, relationship, etc.) does not exist.
 * Maps to HTTP 404 in {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
