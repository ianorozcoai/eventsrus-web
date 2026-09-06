package com.web.eventsrus.backend;

/** Wraps a non-2xx response from the real eventsrus-backend, carrying its "message" field through. */
public class BackendApiException extends RuntimeException {

    public BackendApiException(String message) {
        super(message);
    }
}
