package dev.portfolio.payment.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME ="X-Correlation-ID";
    private static final String MDC_KEY ="correlationId";
    private static final String VALID_ID_PATTERN ="[A-Za-z0-9._-]{1,100}";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        long startTime = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs =(System.nanoTime() - startTime) / 1_000_000;

            log.info("HTTP request completed: " + "method={}, path={}, status={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);

            MDC.remove(MDC_KEY);
        }

    }

    private String resolveCorrelationId(String requestedId) {
        if (requestedId == null || !requestedId.matches(VALID_ID_PATTERN)) {
            return UUID.randomUUID().toString();
        }

        return requestedId;
    }
}