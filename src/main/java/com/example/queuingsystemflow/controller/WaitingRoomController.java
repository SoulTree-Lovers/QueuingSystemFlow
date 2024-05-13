package com.example.queuingsystemflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class WaitingRoomController {

    @GetMapping("/waiting-room")
    Mono<Rendering> waitingRoomPage(
        @RequestParam(name = "queue", defaultValue = "default") String queue,
        @RequestParam(name = "user_id") String userId
    ) {
        // waiting-room.html 렌더링 뷰 리턴
        return Mono.just(Rendering.view("waiting-room.html")
            .build());
    }
}
