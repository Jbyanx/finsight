// ResourceNotFoundException.java  — genérica para Fase 2
package dev.jbyanx.finsight.shared.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}