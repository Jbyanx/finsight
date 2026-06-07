// TokenExpiredException.java
package dev.jbyanx.finsight.shared.exception;

public class TokenExpiredException extends TokenException {
    public TokenExpiredException() {
        super("Token has expired");
    }
}