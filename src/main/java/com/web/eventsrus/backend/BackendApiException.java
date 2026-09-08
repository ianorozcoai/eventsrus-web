package com.web.eventsrus.backend;

/** Wraps a non-2xx response from the real eventsrus-backend, carrying its "message" field through. */
public class BackendApiException extends RuntimeException {

    // The real HTTP status eventsrus-backend responded with. Needed
    // specifically to tell "your JWT expired/was rejected" (401/403) apart
    // from every other failure - see GlobalExceptionHandler, which redirects
    // to login only for those two, and otherwise leaves existing per-call
    // try/catch (packagesError, settingsError, ...) handling exactly as it
    // was.
    private final int status;

    public BackendApiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
