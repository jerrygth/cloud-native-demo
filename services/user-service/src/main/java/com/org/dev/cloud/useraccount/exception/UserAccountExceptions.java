package com.org.dev.cloud.useraccount.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class UserAccountExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.GONE)
    public static class AccountDeletedException extends RuntimeException {
        public AccountDeletedException(String message) {
            super(message);
        }
    }
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccountSuspendedException extends RuntimeException {
        public AccountSuspendedException(String message) {
            super(message);
        }
    }


}
