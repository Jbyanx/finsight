package dev.jbyanx.finsight.shared.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DebtAlreadyPaidOffException.class)
    public ResponseEntity<ErrorResponse> handleDebtPaidOff(
            DebtAlreadyPaidOffException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Debt Already Paid Off", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentExceedsBalanceException.class)
    public ResponseEntity<ErrorResponse> handlePaymentExceedsBalance(
            PaymentExceedsBalanceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Payment Exceeds Balance",
                ex.getMessage(), request);
    }

    @ExceptionHandler(InvoiceAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceAlreadyPaid(
            InvoiceAlreadyPaidException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Invoice Already Paid", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCreditLimitException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCreditLimit(
            InvalidCreditLimitException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Credit Limit", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Insufficient Funds",
                ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Business Rule Violation",
                ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMissingBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Request body is required", request);
    }

    // --- Excepciones de Token ---

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpired(
            TokenExpiredException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Expired", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRevoked(
            TokenRevokedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Revoked", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalid(
            TokenInvalidException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Invalid", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponse> handleTokenException(
            TokenException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Error", ex.getMessage(), request);
    }

    // --- Excepciones JWT de JJWT ---

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Expired", "JWT token has expired", request);
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<ErrorResponse> handleInvalidJwt(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token Invalid", "JWT token is invalid", request);
    }

    // --- Excepciones de recursos ---

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // --- Excepciones de seguridad de Spring ---

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                "You don't have permission to access this resource", request);
    }

    // --- Validación de @Valid ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + " | " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "Validation Error", message, request);
    }

    // --- Fallback general ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", request);
    }

    // --- Helper ---

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                ErrorResponse.of(status.value(), error, message, request.getRequestURI())
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "User Already Exists", ex.getMessage(), request);
    }
}