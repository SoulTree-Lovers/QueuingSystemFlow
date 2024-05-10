package com.example.queuingsystemflow.controller;

import com.example.queuingsystemflow.dto.RegisterUserResponse;
import com.example.queuingsystemflow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

}
