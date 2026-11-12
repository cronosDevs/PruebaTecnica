package com.example.demo.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import com.example.demo.logging.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex) {
        log.info("Solicitud rechazada: codigo={}, detalle={}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        return build(ErrorCode.CREDENCIALES_INVALIDAS);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabled(DisabledException ex) {
        return build(ErrorCode.USUARIO_INACTIVO);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        return build(ErrorCode.NO_AUTENTICADO);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Object> handleExpiredToken(ExpiredJwtException ex) {
        return build(ErrorCode.TOKEN_EXPIRADO);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Object> handleInvalidToken(JwtException ex) {
        return build(ErrorCode.TOKEN_INVALIDO);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.ACCESO_DENEGADO);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String campo = null;
            for (var node : violation.getPropertyPath()) {
                campo = node.getName();
            }
            errores.putIfAbsent(campo, violation.getMessage());
        }
        return build(ErrorCode.VALIDACION_FALLIDA, ErrorCode.VALIDACION_FALLIDA.getMensaje(), errores);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Modificación concurrente detectada: {}", ex.getMessage());
        return build(ErrorCode.MODIFICACION_CONCURRENTE);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraint = constraintName(ex);
        log.warn("Violación de integridad de datos: restriccion={}, causa={}",
                constraint, ex.getMostSpecificCause().getMessage());
        return switch (constraint == null ? "" : constraint) {
            case "sales_idempotencia_uk" -> build(ErrorCode.SOLICITUD_DUPLICADA);
            case "users_username_uk" -> build(ErrorCode.RECURSO_DUPLICADO, "Ya existe un usuario con ese username", null);
            case "users_email_uk" -> build(ErrorCode.RECURSO_DUPLICADO, "Ya existe un usuario con ese email", null);
            default -> build(ErrorCode.CONFLICTO_DE_DATOS);
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.ERROR_INTERNO);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errores.putIfAbsent(globalError.getObjectName(), globalError.getDefaultMessage());
        }
        log.debug("Validación fallida: campos={}", errores.keySet());
        return build(ErrorCode.VALIDACION_FALLIDA, ErrorCode.VALIDACION_FALLIDA.getMensaje(), errores);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return build(ErrorCode.SOLICITUD_INVALIDA, "El cuerpo de la solicitud no es válido o está mal formado", null);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        String detalle = "El valor '%s' no es válido para el parámetro '%s'".formatted(ex.getValue(), parametro);
        return build(ErrorCode.SOLICITUD_INVALIDA, detalle, null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ErrorCode code = ErrorCode.fromStatus(statusCode);
        ProblemDetail problem = body instanceof ProblemDetail existing
                ? existing
                : ProblemDetail.forStatus(statusCode);
        problem.setDetail(code.getMensaje());
        enrich(problem, code);
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }

    private ResponseEntity<Object> build(ErrorCode code) {
        return build(code, code.getMensaje(), null);
    }

    private ResponseEntity<Object> build(ErrorCode code, String detail, @Nullable Map<String, String> errores) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        enrich(problem, code);
        if (errores != null && !errores.isEmpty()) {
            problem.setProperty("errores", errores);
        }
        return ResponseEntity.status(code.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private void enrich(ProblemDetail problem, ErrorCode code) {
        problem.setTitle(code.getTitulo());
        problem.setProperty("codigo", code.name());
        problem.setProperty("fecha", OffsetDateTime.now());
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
    }

    @Nullable
    private static String constraintName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
