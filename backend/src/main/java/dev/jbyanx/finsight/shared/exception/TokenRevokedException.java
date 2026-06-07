// TokenRevokedException.java
package dev.jbyanx.finsight.shared.exception;

public class TokenRevokedException extends TokenException {
    public TokenRevokedException() {
        super("Token has been revoked");
    }
}