package com.example.ratelimiter.config;

import com.example.ratelimiter.algorithm.fixedwindow.RedisFixedWindowCounter;
import com.example.ratelimiter.algorithm.leakybucket.InMemoryLeakyBucketStore;
import com.example.ratelimiter.RateLimitAlgorithm;
import com.example.ratelimiter.RateLimitFilter;
import com.example.ratelimiter.RateLimiterRegistry;
import com.example.ratelimiter.interfaces.FixedWindowCounter;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import com.example.ratelimiter.props.RateLimiterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPooled;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimitAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public JedisPooled rateLimiterRedisClient(RateLimiterProperties properties) {
        return new JedisPooled(properties.getRedisUrl());
    }

    @Bean
    @ConditionalOnMissingBean(FixedWindowCounter.class)
    public FixedWindowCounter rateLimiterCounter(JedisPooled rateLimiterRedisClient) {
        return new RedisFixedWindowCounter(rateLimiterRedisClient);
    }

    @Bean
    @ConditionalOnMissingBean(LeakyBucketStore.class)
    public LeakyBucketStore leakyBucketStore() {
        return new InMemoryLeakyBucketStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry(
            RateLimiterProperties properties,
            FixedWindowCounter counter,
            LeakyBucketStore leakyBucketStore) {
        RateLimitAlgorithm algorithm = RateLimitAlgorithm.parse(properties.getAlgorithm());
        return new RateLimiterRegistry(
                algorithm, properties.getConsumers(), counter, leakyBucketStore);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimiterRegistry registry, RateLimiterProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(registry, properties.getConsumerIdHeader()));
        registration.addUrlPatterns("/*");
        registration.setName("consumerRateLimitFilter");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
