package com.project.familytree.tree.exceptions;

/**
 * Thrown for business-rule violations that should return HTTP 400 Bad Request.
 * Examples: duplicate relationship, editing a deleted comment, etc.
 * Maps to HTTP 400 in {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
