package com.example.internaldeveloperportal.exception;

/** Raised when a request violates a uniqueness or state constraint. */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
