package barbearia_api.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/** Limites simples por endereço de origem para evitar abuso dos endpoints públicos. */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_CLIENTS = 20_000;
    private final ObjectMapper objectMapper;
    private final int loginLimit;
    private final long loginWindowMs;
    private final int registrationLimit;
    private final long registrationWindowMs;
    private final boolean trustForwardedHeaders;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(ObjectMapper objectMapper,
            @Value("${security.rate-limit.login.requests:10}") int loginLimit,
            @Value("${security.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${security.rate-limit.registration.requests:5}") int registrationLimit,
            @Value("${security.rate-limit.registration.window-seconds:600}") long registrationWindowSeconds,
            @Value("${security.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.objectMapper = objectMapper;
        this.loginLimit = loginLimit;
        this.loginWindowMs = loginWindowSeconds * 1000;
        this.registrationLimit = registrationLimit;
        this.registrationWindowMs = registrationWindowSeconds * 1000;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        int limit;
        long period;
        if ("/auth/login".equals(path)) {
            limit = loginLimit;
            period = loginWindowMs;
        } else if ("/auth/cadastro".equals(path)) {
            limit = registrationLimit;
            period = registrationWindowMs;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = path + ":" + clientAddress(request);
        if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= period * 2);
            if (windows.size() >= MAX_TRACKED_CLIENTS) {
                windows.entrySet().stream()
                        .min(java.util.Comparator.comparingLong(entry -> entry.getValue().startedAt))
                        .ifPresent(entry -> windows.remove(entry.getKey(), entry.getValue()));
            }
        }
        Window current = windows.compute(key, (ignored, existing) ->
                existing == null || now - existing.startedAt >= period
                        ? new Window(now) : existing);
        boolean allowed = current.acquire(limit);
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(Math.max(1,
                    (current.startedAt + period - now + 999) / 1000)));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Muitas requisicoes. Aguarde antes de tentar novamente.",
                    "path", path,
                    "timestamp", Instant.now().toString()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt) { this.startedAt = startedAt; }
        private synchronized boolean acquire(int limit) { return ++count <= limit; }
    }
}
