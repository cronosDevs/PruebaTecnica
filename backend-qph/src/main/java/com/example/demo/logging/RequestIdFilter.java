package com.example.demo.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    public static final String USER_MDC_KEY = "usuario";

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String recibido = request.getHeader(HEADER);
        String requestId = recibido != null && VALID_REQUEST_ID.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        long inicio = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!isDocumentation(request.getRequestURI())) {
                long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
                log.info("{} {} -> {} ({} ms) ip={}", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), duracionMs, request.getRemoteAddr());
            }
            MDC.remove(USER_MDC_KEY);
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean isDocumentation(String uri) {
        return uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }
}
