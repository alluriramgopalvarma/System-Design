package com.example.ratelimiter;

import com.example.ratelimiter.interfaces.RateLimiter;
import com.example.ratelimiter.props.RateLimiterProperties;
import java.io.IOException;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public final class RateLimitFilter extends OncePerRequestFilter {
    private static final int SC_TOO_MANY_REQUESTS = 429;

    private final RateLimiterRegistry registry;
    private final String consumerIdHeader;

    public RateLimitFilter(RateLimiterRegistry registry, String consumerIdHeader) {
        this.registry = Objects.requireNonNull(registry, "registry");
        if (consumerIdHeader == null || consumerIdHeader.trim().isEmpty()) {
            throw new IllegalArgumentException("consumerIdHeader must not be empty");
        }
        this.consumerIdHeader = consumerIdHeader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String consumerId = request.getHeader(consumerIdHeader);
        if (consumerId == null || consumerId.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing consumer ID header: " + consumerIdHeader);
            return;
        }

        RateLimiter limiter = registry.get(consumerId);
        if (limiter == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unknown consumer");
            return;
        }

        RateLimitDecision decision = limiter.check(consumerId);
        response.setHeader("X-RateLimit-Limit", Long.toString(decision.getLimit()));
        response.setHeader("X-RateLimit-Remaining", Long.toString(decision.getRemaining()));
        response.setHeader("X-RateLimit-Reset-Ms", Long.toString(decision.getRetryAfterMillis()));
        if (!decision.isAllowed()) {
            response.setHeader("Retry-After",
                    Long.toString((decision.getRetryAfterMillis() - 1) / 1000 + 1));
            response.sendError(SC_TOO_MANY_REQUESTS, "Rate limit exceeded");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
