package com.porterclone.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown whenever a state-machine transition is attempted that isn't legal from the current state. */
public class InvalidStateTransitionException extends ApiException {
    public InvalidStateTransitionException(String entity, String from, String to) {
        super(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                String.format("%s cannot transition from %s to %s", entity, from, to));
    }
}
