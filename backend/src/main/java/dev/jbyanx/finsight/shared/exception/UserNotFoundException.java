// UserNotFoundException.java
package dev.jbyanx.finsight.shared.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}