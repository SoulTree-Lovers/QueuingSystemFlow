package com.example.queuingsystemflow.service;

import com.example.queuingsystemflow.EmbeddedRedis;
import com.example.queuingsystemflow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test") // application.yaml에서 설정한 포트번호 63790 사용
class UserQueueServiceTest {

    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    public void beforeEach() {
        // 각 단위 테스트 시작 전 데이터 전부 삭제
        ReactiveRedisConnection reactiveConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void registerWaitQueue() {
        // 대기열 등록 테스트
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
            .expectNext(1L)
            .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 101L))
            .expectNext(2L)
            .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 102L))
            .expectNext(3L)
            .verifyComplete();
    }

    @Test
    void alreadyRegisterWaitQueue() {
        // 이미 등록된 사용자 재등록 테스트
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
            .expectNext(1L)
            .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
            .expectError(ApplicationException.class)
            .verify();
    }

    @Test
    void emptyAllowUser() {
        StepVerifier.create(userQueueService.allowUser("default", 3L))
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    void allowUser() {
        // 3명 유저 등록 후 2명 접속 허용 요청 -> 2 리턴
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
            .then(userQueueService.registerWaitQueue("default", 101L))
            .then(userQueueService.registerWaitQueue("default", 102L))
            .then(userQueueService.allowUser("default", 2L)))
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    void allowUser2() {
        // 3명 유저 등록 후 5명 접속 허용 요청 -> 3 리턴
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.registerWaitQueue("default", 101L))
                .then(userQueueService.registerWaitQueue("default", 102L))
                .then(userQueueService.allowUser("default", 5L)))
            .expectNext(3L)
            .verifyComplete();
    }

    @Test
    void allowUserAfterRegisterWaitQueue() {
        // 3명 유저 등록 -> 5명 접속 허용 요청 -> 1명 유저 등록 -> 1 리턴 (대기번호 1)
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.registerWaitQueue("default", 101L))
                .then(userQueueService.registerWaitQueue("default", 102L))
                .then(userQueueService.allowUser("default", 5L))
                .then(userQueueService.registerWaitQueue("default", 200L)))
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    void isNotAllowed() {
        // 접속 가능 대기열에 아무것도 없는 상태로 접속 가능 여부 조회 -> false
        StepVerifier.create(userQueueService.isAllowed("default", 100L))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void isNotAllowed2() {
        // 100번 유저를 허용시킨 후, 101번 유저 접속 가능 여부 조회 -> false
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.allowUser("default", 1L))
                .then(userQueueService.isAllowed("default", 101L)))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void isAllowed() {
        // 100번 유저를 허용시킨 후, 101번 유저 접속 가능 여부 조회 -> false
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.allowUser("default", 1L))
                .then(userQueueService.isAllowed("default", 100L)))
            .expectNext(true)
            .verifyComplete();
    }


}