package com.example.ratelimiter;

import com.example.ratelimiter.interfaces.FixedWindowCounter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.ratelimiter.demo.RateLimiterDemoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = RateLimiterDemoApplication.class,
        properties = {
                "rate-limiter.consumers.demo-client.limit=2",
                "rate-limiter.consumers.partner-client.limit=10",
                "logging.level.root=ERROR"
        })
@AutoConfigureMockMvc
@Import(RateLimiterHttpIntegrationTest.CounterConfiguration.class)
class RateLimiterHttpIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredConsumersCanCallEndpointUpToTheirLimit() throws Exception {
        mockMvc.perform(get("/api/demo").header("X-Consumer-Id", "demo-client"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "1"))
                .andExpect(jsonPath("$.message").value("Request passed through the rate limiter"));

        mockMvc.perform(get("/api/demo").header("X-Consumer-Id", "demo-client"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        mockMvc.perform(get("/api/demo").header("X-Consumer-Id", "demo-client"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"));

        mockMvc.perform(get("/api/demo").header("X-Consumer-Id", "partner-client"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "10"));
    }

    @Test
    void rejectsMissingAndUnconfiguredConsumers() throws Exception {
        mockMvc.perform(get("/api/demo"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/demo").header("X-Consumer-Id", "unknown"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class CounterConfiguration {
        @Bean
        FixedWindowCounter testFixedWindowCounter() {
            ConcurrentHashMap<String, AtomicLong> counts = new ConcurrentHashMap<>();
            return (key, ttlMillis) -> counts.computeIfAbsent(key, ignored -> new AtomicLong())
                    .incrementAndGet();
        }
    }
}
