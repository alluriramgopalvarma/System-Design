# `com.example.ratelimiter`

Core types shared by the rate-limiting algorithms and Spring integration.

- `RateLimitFilter` reads the configured consumer ID header, rejects missing
  IDs with HTTP 400 and unknown consumers with HTTP 403, and returns HTTP 429
  when a consumer exceeds its limit.
- `RateLimiterRegistry` maps configured consumer IDs to their rate limiters.
- `RateLimitDecision` carries the allowed status, limit, remaining capacity,
  and retry delay.
- `RateLimitAlgorithm` names the algorithms currently selected by application
  configuration: `FIXED_WINDOW` and `LEAKY_BUCKET`.

Algorithm implementations are under `algorithm`; Spring setup and settings are
under `config` and `props`. Shared contracts and limiter selection are in
`interfaces` and `factory`, and the sample application is in `demo` and
`controller`.
