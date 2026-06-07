// TokenInvalidException.java
package dev.jbyanx.finsight.shared.exception;

public class TokenInvalidException extends TokenException {
    public TokenInvalidException() {
        super("Token is invalid");
    }
}