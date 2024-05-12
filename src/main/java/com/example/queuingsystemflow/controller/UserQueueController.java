package com.example.queuingsystemflow.controller;

import com.example.queuingsystemflow.dto.AllowUserResponse;
import com.example.queuingsystemflow.dto.AllowedUserResponse;
import com.example.queuingsystemflow.dto.RegisterUserResponse;
import com.example.queuingsystemflow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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


}
