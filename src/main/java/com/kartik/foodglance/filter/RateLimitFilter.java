package com.kartik.foodglance.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String  RATE_LIMITED_PATH = "/detect-food";
    private static final int     MAX_REQUESTS      = 10;
    private static final Duration REFILL_PERIOD    = Duration.ofMinutes(1);

    // One bucket per IP — expires 1 hour after last access, so memory doesn't grow forever
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        Bucket bucket = buckets.get(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", ip);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"You have exceeded the limit of " + MAX_REQUESTS +
                " requests per minute. Please wait and try again.\"}"
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to /detect-food — skip all other endpoints
        return !request.getRequestURI().equals(RATE_LIMITED_PATH);
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS)
                .refillGreedy(MAX_REQUESTS, REFILL_PERIOD)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        // Respect X-Forwarded-For when behind a proxy/Railway
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
