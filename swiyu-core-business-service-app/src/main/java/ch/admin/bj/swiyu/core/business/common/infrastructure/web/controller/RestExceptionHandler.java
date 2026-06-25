package ch.admin.bj.swiyu.core.business.common.infrastructure.web.controller;

import static ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode.DATA_INVALID;
import static ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode.RESOURCE_NOT_FOUND;
import static ch.admin.bj.swiyu.core.business.common.infrastructure.web.controller.RestExceptionMapper.toApiErrorDto;
import static ch.admin.bj.swiyu.core.business.common.infrastructure.web.controller.RestExceptionMapper.toFieldErrorDetails;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.*;
import ch.admin.bj.swiyu.registry.identifier.common.exception.DidEntityNotFoundException;
import ch.admin.bj.swiyu.registry.identifier.common.exception.DidEntityNotReadyException;
import ch.admin.bj.swiyu.registry.status.common.exception.StatusListNotFoundException;
import ch.admin.bj.swiyu.registry.status.common.exception.StatusListNotReadyException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ OptimisticLockException.class, ObjectOptimisticLockingFailureException.class })
    public ResponseEntity<ApiErrorDto> handleOptimisticLockException(final RuntimeException e) {
        log.warn("Optimistic lock exception", e);
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> handleDataIntegrityViolation(final DataIntegrityViolationException e) {
        log.warn("Data integrity violation detected.", e);
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiErrorDto> handlePropertyReferenceException(final PropertyReferenceException e) {
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ MaxSizeApiException.class })
    public ResponseEntity<ApiErrorDto> handleMaxSizeApiException(final BusinessException e) {
        return new ResponseEntity<>(
            toApiErrorDto(e.getErrorCode(), e.getMessage(), e.getAdditionalDetails()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(
        {
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class,
            PartnerIsNotGovernmentalException.class,
        }
    )
    public ResponseEntity<ApiErrorDto> handleAccessDenied(final RuntimeException e) {
        log.debug("Detected unauthorized access.", e);
        return new ResponseEntity<>(
            toApiErrorDto(BusinessExceptionErrorCode.RESOURCE_FORBIDDEN, e.getMessage()),
            HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(
        { ResourceNotFoundException.class, StatusListNotFoundException.class, DidEntityNotFoundException.class }
    )
    public ResponseEntity<ApiErrorDto> handleNotFound(final RuntimeException e) {
        log.info("User does not find resources.", e);
        return new ResponseEntity<>(toApiErrorDto(RESOURCE_NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ StatusListNotReadyException.class, DidEntityNotReadyException.class })
    public ResponseEntity<ApiErrorDto> handleBadRequest(final RuntimeException e) {
        log.info("Invalid data provided", e);
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ ValidationException.class })
    public ResponseEntity<ApiErrorDto> handleBadRequest(final ValidationException e) {
        log.info("Invalid data provided", e);
        var additionalDetails = e.getViolations().stream().map(ValidationException.Violation::reason).toList();
        return new ResponseEntity<>(
            toApiErrorDto(DATA_INVALID, e.getMessage(), additionalDetails),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BusinessDataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> handleConstraintViolations(final BusinessException e) {
        log.info("DataIntegrity ViolationException created.", e);
        return new ResponseEntity<>(toApiErrorDto(e.getErrorCode(), e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorDto> handleGenericBusinessException(final BusinessException e) {
        log.info("GenericBusinessException occurred.", e);
        var messagesOfExceptionTree = new ArrayList<>(e.getAdditionalDetails());
        if (e instanceof ExposeAdditionalDetailsBusinessException) {
            var cause = e.getCause();
            var maxDepth = 10;
            while (maxDepth > 0 && cause != null) {
                messagesOfExceptionTree.add(cause.getMessage());
                if (cause instanceof BusinessException businessCause) {
                    messagesOfExceptionTree.addAll(businessCause.getAdditionalDetails());
                }
                cause = cause.getCause();
                maxDepth--;
            }
        }
        return new ResponseEntity<>(
            toApiErrorDto(e.getErrorCode(), e.getMessage(), messagesOfExceptionTree),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler({ AccessDeniedException.class })
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleUnauthorizedFileAccess(final AccessDeniedException e) {
        log.error("Detected unauthorized file access.", e);
    }

    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<String> handleExternalSystemException(
        final ExternalSystemException e,
        HttpServletRequest request
    ) {
        if (e.getHttpStatusCode().is4xxClientError()) {
            log.info("Unhandled exception occurred for URL {}", request.getRequestURL(), e);
        } else {
            log.error("Unhandled exception occurred for URL {}", request.getRequestURL(), e);
        }
        return ResponseEntity.status(e.getHttpStatusCode()).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleUnexpectedErrors(final Exception e, HttpServletRequest request) {
        log.error("Detected unhandled exception for URL {}.", request.getRequestURL(), e);
    }

    /**
     * Handles @Valid @RequestBody failures. Collects all field-level constraint violations
     * and returns them as "fieldName: message" entries in additionalDetails.
     * Rejected values are never included to avoid exposing sensitive input.
     */
    @Override
    @SuppressWarnings({ "java:S4144", "java:S2638" }) // always non-null; parent declares @Nullable for delegation use cases we don't need
    @NonNull
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        @NonNull MethodArgumentNotValidException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<String> details = toFieldErrorDetails(fieldErrors);
        log.debug("DTO validation failed with {} field error(s).", fieldErrors.size());
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, "Validation failed", details), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles @Valid on path/query parameters. Collects all parameter-level constraint violations
     * and returns them as "paramName: message" entries in additionalDetails.
     * Rejected values are never included to avoid exposing sensitive input.
     */
    @Override
    @SuppressWarnings("java:S2638") // always non-null; parent declares @Nullable for delegation use cases we don't need
    @NonNull
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
        @NonNull HandlerMethodValidationException ex,
        @NonNull HttpHeaders headers,
        @NonNull HttpStatusCode status,
        @NonNull WebRequest request
    ) {
        List<String> details = Stream.concat(ex.getBeanResults().stream(), ex.getValueResults().stream())
            .flatMap(pvr ->
                pvr
                    .getResolvableErrors()
                    .stream()
                    .map(e -> pvr.getMethodParameter().getParameterName() + ": " + resolveMessage(e))
            )
            .toList();
        log.debug("Method parameter validation failed with {} error(s).", details.size());
        return new ResponseEntity<>(toApiErrorDto(DATA_INVALID, "Validation failed", details), HttpStatus.BAD_REQUEST);
    }

    private static String resolveMessage(MessageSourceResolvable resolvable) {
        String msg = resolvable.getDefaultMessage();
        return msg != null ? msg : "invalid value";
    }
}
