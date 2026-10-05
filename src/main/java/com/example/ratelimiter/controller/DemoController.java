package com.example.ratelimiter.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    @GetMapping("/api/demo")
    public Map<String, String> demo() {
        return Collections.singletonMap("message", "Request passed through the rate limiter");
    }
}
