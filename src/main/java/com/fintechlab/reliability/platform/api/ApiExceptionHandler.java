package com.fintechlab.reliability.platform.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = baseProblem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "The request body or parameters failed validation.",
                ApiErrorCode.VALIDATION_FAILED,
                request);

        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return baseProblem(
                HttpStatus.BAD_REQUEST,
                "Bad request",
                exception.getMessage(),
                ApiErrorCode.BAD_REQUEST,
                request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception", exception);
        return baseProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred.",
                ApiErrorCode.INTERNAL_ERROR,
                request);
    }

    private static ProblemDetail baseProblem(
            HttpStatus status,
            String title,
            String detail,
            ApiErrorCode code,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://fintech-reliability-lab.local/problems/" + code.name().toLowerCase()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        problem.setProperty("correlationId", CorrelationIdFilter.currentCorrelationId());
        problem.setProperty("retryable", status.is5xxServerError());
        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}
