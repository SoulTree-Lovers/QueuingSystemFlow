package com.example.queuingsystemflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import static com.example.queuingsystemflow.exception.ErrorCode.QUEUE_ALREADY_REGISTERED_USER;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final String USER_QUEUE_WAIT_KEY = "users:queue:%s:wait"; // 사용자 대기 큐
    private final String USER_QUEUE_WAIT_KEY_FOR_SCAN = "users:queue:*:wait"; // 사용자 대기 큐 스캔
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:%s:proceed"; // 사용자 접속 허용 큐

    @Value("${scheduler.enabled}") // application.yaml의 scheduler.enabled 값을 가져오도록 설정
    private Boolean scheduling = false;

    // 대기열 등록 API
    public Mono<Long> registerWaitQueue(final String queue, final Long userId) {
        /**
         * redis sortedset에 저장
         * - key: userId
         * - value: unix timestamp
         * - rank: 몇 번째 대기 순서인지
         */
        var unixTimestamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString(), unixTimestamp)
            .filter(i -> i)
            .switchIfEmpty(Mono.error(QUEUE_ALREADY_REGISTERED_USER.build()))
            .flatMap(i -> reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString()))
            .map(i -> i >= 0 ? i+1: i)
            ;

    }

    // 진입을 허용하는 메소드
    public Mono<Long> allowUser(final String queue, final Long count) {
        // 진입을 허용하는 단계
        // 1. wait queue에서 사용자를 제거
        // 2. proceed queue에 사용자를 추가
        return reactiveRedisTemplate.opsForZSet().popMin(USER_QUEUE_WAIT_KEY.formatted(queue), count) // count 개수 만큼 사용자 제거
            .flatMap(member -> reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY.formatted(queue), member.getValue(), Instant.now().getEpochSecond()))
            .count();
    }

    // 진입이 가능한 상태인지 조회 (특정 사용자가 진입 가능 큐에 존재하는지 확인)
    public Mono<Boolean> isAllowed(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_PROCEED_KEY.formatted(queue), userId.toString())
            .defaultIfEmpty(-1L) // 값이 없다면 -1 리턴 (등록되지 않음)
            .map(rank -> rank >= 0); // rank가 0보다 크다면 등록된 것으로 간주
    }

    // 토큰을 통해 접속 가능 여부 조회
    public Mono<Boolean> isAllowedByToken(final String queue, final Long userId, final String token) {
        return this.generateToken(queue, userId)
            .filter(gen -> gen.equalsIgnoreCase(token))
            .map(i -> true) // 토큰 값이 같다면 true 리턴 (토큰 검증 성공)
            .defaultIfEmpty(false);
    }

    public Mono<Long> getRank(final String queue, final Long userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY.formatted(queue), userId.toString())
            .defaultIfEmpty(-1L) // 값이 없다면 -1 리턴 (등록되지 않음)
            .map(rank -> rank >= 0 ? rank + 1 : rank); // ex. 0번째 대기자 -> 1번째 대기자
    }

    public Mono<String> generateToken(final String queue, final Long userId)  {
        // sha256 해시 데이터 생성
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");

            var input = "user-queue-%s-%d".formatted(queue, userId); // token 생성 문자열
            byte[] encodeHash = digest.digest(input.getBytes(StandardCharsets.UTF_8)); // 해시 값 생성

            StringBuilder hexString = new StringBuilder();
            for (byte aByte : encodeHash) { // byte를 하나씩 hex 포멧으로 추가
                hexString.append(String.format("%02x", aByte));
            }

            return Mono.just(hexString.toString());

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 3000) // 서버 시작 후 5초 이후부터 스케쥴 동작, 3초 주기로 아래 메소드 실행
    public void scheduleAllowUser() {
        if (!scheduling) {
            log.info("passed scheduling ...");
            return ;
        }

        log.info("called scheduling ...");

        Long maxAllowUserCount = 3L; // 한 번에 허용시킬 유저 수

        // 사용자를 허용하는 로직
        reactiveRedisTemplate.scan(ScanOptions.scanOptions()
                .match(USER_QUEUE_WAIT_KEY_FOR_SCAN)
                .count(100) // 최대 100개의 대기 키 조회
                .build()
            )
            .map(key -> key.split(":")[2]) // * 부분 조회
            .flatMap(queue -> allowUser(queue, maxAllowUserCount).map(allowed -> Tuples.of(queue, allowed)))
            .doOnNext(tuple -> log.info("Tried %d and allowed %d members of %s queue".formatted(maxAllowUserCount, tuple.getT2(), tuple.getT1())))
            .subscribe();
    }
}
