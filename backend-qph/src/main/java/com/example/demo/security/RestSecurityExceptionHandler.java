package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
public class RestSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public RestSecurityExceptionHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        log.debug("Acceso sin autenticación: {} {}", request.getMethod(), request.getRequestURI());
        handlerExceptionResolver.resolveException(request, response, null, authException);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        log.warn("Acceso denegado: {} {}", request.getMethod(), request.getRequestURI());
        handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
    }
}
