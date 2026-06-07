// UnauthorizedException.java
package dev.jbyanx.finsight.shared.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}