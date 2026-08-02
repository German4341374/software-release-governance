package com.portfolio.releasegovernance.api;

import com.portfolio.releasegovernance.adapter.VersionSourceException;
import com.portfolio.releasegovernance.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(GovernanceRuleException.class)
    ResponseEntity<ProblemDetail> rule(GovernanceRuleException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler(VersionSourceException.class)
    ResponseEntity<ProblemDetail> source(VersionSourceException exception, HttpServletRequest request) {
        HttpStatus status = exception.isRateLimited() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_GATEWAY;
        ResponseEntity<ProblemDetail> response = problem(status,
                exception.isRateLimited() ? "SOURCE_RATE_LIMITED" : "SOURCE_UNAVAILABLE",
                exception.getMessage(), request);
        if (exception.getRetryAfter() != null) response.getBody().setProperty("retryAfter", exception.getRetryAfter());
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), ignored -> new ArrayList<>()).add(error.getDefaultMessage());
        }
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more request fields are invalid.", request);
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> invalid(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> conflict(RuntimeException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "CONCURRENCY_CONFLICT",
                "The resource changed after it was read. Reload and retry.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> constraint(DataIntegrityViolationException exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "DATABASE_CONSTRAINT",
                "A uniqueness or relationship constraint rejected the request.", request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://example.invalid/problems/" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
