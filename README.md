# Redis Rate Limiter

A Java 11 / Spring Boot 2.7 library that applies per-consumer fixed-window
limits to incoming servlet HTTP requests. Redis stores atomic counters, so
limits are shared between application instances. Consumer rules and the
consumer ID header are supplied by the integrating application; the library
does not require application code changes beyond adding the dependency and
configuration.

## Try the included API

Start Redis:

```sh
docker compose up -d --build
```

Start the sample API (requires Java 11+; the Maven Wrapper downloads Maven on
its first run, so a global Maven installation is not required):

```sh
./mvnw spring-boot:run
```

Call the endpoint with a configured consumer ID:

```sh
curl -i -H 'X-Consumer-Id: demo-client' http://localhost:8080/api/demo
```

The sample configures `demo-client` for 3 requests per minute and
`partner-client` for 10 requests per minute. Repeat the demo-client request
until it returns HTTP `429`. The response includes `X-RateLimit-Limit`,
`X-RateLimit-Remaining`, `X-RateLimit-Reset-Ms`, and `Retry-After` headers.
Missing consumer IDs return `400`; unconfigured IDs return `403`.

Run all unit and HTTP integration tests with:

```sh
./mvnw test
```

The HTTP integration test substitutes an in-memory counter, so tests do not
require Redis or Docker.

## Add the library to a Spring Boot application

Build and install the library locally:

```sh
./mvnw clean install
```

Add the dependency to the other project's `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>redis-rate-limiter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Set the Redis URL and per-consumer rules in the consuming application's
`application.yml`:

```yaml
rate-limiter:
  algorithm: FIXED_WINDOW
  redis-url: redis://localhost:6379
  consumer-id-header: X-Consumer-Id
  consumers:
    mobile-app:
      limit: 100
      window: 1m
    partner-api:
      limit: 1000
      window: 1h
```

The library auto-configures the Redis client, strategy, consumer registry, and
servlet filter. Include the consumer ID header on requests; no controller
changes are needed. Each configured consumer gets an independent limit. Unknown
consumers are rejected by default instead of silently bypassing limits. Set
`rate-limiter.algorithm` to `FIXED_WINDOW` to use the implemented strategy.
Algorithm implementations are organized under
`com.example.ratelimiter.algorithm.fixedwindow` and
`com.example.ratelimiter.algorithm.leakybucket`. Spring configuration and
configuration properties live under `com.example.ratelimiter.config` and
`com.example.ratelimiter.props`. Shared contracts are in
`com.example.ratelimiter.interfaces`, factory/strategy selection is in
`com.example.ratelimiter.factory`, and the sample endpoint is in
`com.example.ratelimiter.controller`. `LocalRunner` starts the sample API with
local demo defaults.

## Leaky-bucket implementation exercise

The leaky-bucket strategy uses an in-memory FIFO store selected by the algorithm
and per-consumer configuration, but its core logic is intentionally left for
you to implement.
Select it with:

```yaml
rate-limiter:
  algorithm: LEAKY_BUCKET
  redis-url: redis://localhost:6379
  consumers:
    mobile-app:
      capacity: 5
      leak-rate-per-second: 5
```

Here, `capacity` is the maximum queued request count and
`leak-rate-per-second` is the number of queued requests drained per second
(`5` means one request drains about every `200 ms`). The
TODOs in `InMemoryLeakyBucketStore` guide the FIFO queue operations, and
the TODO in `LeakyBucketRateLimiter` guides decision handling. Implement both
before sending requests with this algorithm—the TODO methods throw explicitly
until completed. This leaky-bucket store is in-memory and local to one process;
fixed-window mode continues to use Redis.

The lower-level API is also available for non-HTTP code:

```java
RateLimitDecision decision = rateLimiterRegistry.get("mobile-app").check("mobile-app");
if (decision.isAllowed()) {
    // continue processing
}
```
