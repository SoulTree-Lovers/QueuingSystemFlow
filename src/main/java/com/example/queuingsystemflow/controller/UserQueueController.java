package com.example.queuingsystemflow.controller;

import com.example.queuingsystemflow.dto.AllowUserResponse;
import com.example.queuingsystemflow.dto.AllowedUserResponse;
import com.example.queuingsystemflow.dto.RankNumberResponse;
import com.example.queuingsystemflow.dto.RegisterUserResponse;
import com.example.queuingsystemflow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {

    private final UserQueueService userQueueService;

    // 등록할 수 있는 API path
    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
        @RequestParam(name = "user_id") Long userId,
        @RequestParam(defaultValue = "default") String queue
    ) {
        return userQueueService.registerWaitQueue(queue, userId)
            .map(RegisterUserResponse::new);
    }

    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUser(
        @RequestParam(defaultValue = "default") String queue,
        @RequestParam(defaultValue = "count") Long count
    ) {
        return userQueueService.allowUser(queue, count)
            .map(allowed -> new AllowUserResponse(count, allowed));
    }

    @GetMapping("/allowed")
    public Mono<?> isAllowedUser(
        @RequestParam(name = "user_id") Long userId,
        @RequestParam(defaultValue = "default") String queue
    ) {
        return userQueueService.isAllowed(queue, userId)
            .map(AllowedUserResponse::new);
    }

    @GetMapping("/rank")
    public Mono<RankNumberResponse> getUserRank(
        @RequestParam(name = "user_id") Long userId,
        @RequestParam(defaultValue = "default") String queue
    ) {
        return userQueueService.getRank(queue, userId)
            .map(RankNumberResponse::new);
    }

    @GetMapping("/touch")
    Mono<String> touch(
        @RequestParam(name = "user_id") Long userId,
        @RequestParam(defaultValue = "default") String queue,
        ServerWebExchange exchange
    )  {
        return Mono.defer(() -> userQueueService.generateToken(queue, userId))
            .map(token -> {
                exchange.getResponse().addCookie(
                    ResponseCookie.from("user-queue-%s-token".formatted(queue), token)
                        .maxAge(Duration.ofSeconds(300)) // 5분 간 토큰 유지
                        .path("/")
                        .build()
                );

                return token;
            });
    }
}
