package com.example.queuingsystemflow.controller;

import com.example.queuingsystemflow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class WaitingRoomController {

    private final UserQueueService userQueueService;

    @GetMapping("/waiting-room")
    Mono<Rendering> waitingRoomPage(
        @RequestParam(name = "queue", defaultValue = "default") String queue,
        @RequestParam(name = "user_id") Long userId,
        @RequestParam(name = "redirect_url") String redirectUrl,
        ServerWebExchange exchange
    ) {
        var tokenKey = "user-queue-%s-token".formatted(queue); // 토큰 키 생성
        var cookieValue = exchange.getRequest().getCookies().getFirst(tokenKey); // 쿠키 가져오기
        var token = cookieValue == null ? "" : cookieValue.getValue(); // 쿠키 값이 있다면 토큰 값 가져오기

        // 1. 입장이 허용되어 page redirect(이동)가 가능한 상태인가?
        return userQueueService.isAllowedByToken(queue, userId, token)
            .filter(allowed -> allowed) // 입장이 허용된다면
            // 2. 어디로 이동해야 하는가?
            .flatMap(allowed -> Mono.just(Rendering.redirectTo(redirectUrl).build())) // 페이지 이동
            .switchIfEmpty(
                // 입장이 허용되지 않았다면, 대기 등록 및 웹페이지에 필요한 데이터 전달
                userQueueService.registerWaitQueue(queue, userId)
                    .onErrorResume(ex -> userQueueService.getRank(queue, userId)) // 등록이 되어있다면, 대기 번호 조회
                    .map(rank -> Rendering.view("waiting-room.html")
                        .modelAttribute("number", rank)
                        .modelAttribute("userId", userId)
                        .modelAttribute("queue", queue)
                        .build()
                )
            );

    }
}
