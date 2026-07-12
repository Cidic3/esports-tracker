package dev.mundorf.esportstracker.exception;

/**
 * Thrown when a client's submitted version doesn't match the current entity version - the
 * follow-update PUTs' way of rejecting a write computed from a stale cached profile rather than
 * silently letting it clobber a change the client hasn't seen yet. See User.version.
 */
public class StaleUpdateException extends RuntimeException {

    public StaleUpdateException(String message) {
        super(message);
    }
}
