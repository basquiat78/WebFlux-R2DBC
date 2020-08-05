package io.basquiat.redis.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReactiveRedisTest {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * redis에서는 rpush, lpush, rpop, lpop을 통해서 stack, queue를 구현할 수 있다.
     * 만일 다음과 같은 것을 생각해 보자.
     *                          index 0, 1, 2, 3, ......
     *                          -------------------------
     *          LEFT POP <--                              <-- RIGHT PUSH DATA
     *                          -------------------------
     *
     *                          -------------------------
     *      LEFT PUSH DATA ->                              --> RIGHT POP
     *                          -------------------------
     * 1. 어떤 비지니스 로직은 무조건 right push, left pop으로만 진행한다. 또는 left push, right pop으로만 진행한다.
     *    - 잘 생각해보면 이것은 오른쪽에서 왼쪽 또는 반대로 흘러가는 queue의 형태와 같다.
     *
     *                               -------------------------
     *    LEFT POP <-- --> LEFT PUSH
     *                               -------------------------
     *
     *              -------------------------
     *                                         RIGHT PUSH <-- --> RIGHT POP
     *              -------------------------
     * 2. 어떤 비지니스 로직은 무조건 right push, left pop으로만 진행한다. 또는 left push, right pop으로만 진행한다.
     *     - 이것도 잘 보면 stack처럼 작동한다.
     *
     * 3. 위와 같은 방식을 통해서 어떤 니즈로 인해서 LPOP을 사용할 것인지 RPOP을 사용할 것인지 정할 수 있다.
     *
     */
    //@Test
    void givenRightPush() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 단건으로 넣기
        Mono<Long> mono = reactiveListOps.rightPush("deliveryList", "delivery1").log("completed right push!");
        StepVerifier.create(mono)
                    .expectNext(1L)
                    .verifyComplete();
    }

    //@Test
    void givenRightPushAll() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 리스트로 통채로 넣기
        Mono<Long> mono = reactiveListOps.rightPushAll("deliveryList", Arrays.asList("delivery2", "delivery3", "delivery4"))
                                         .log("All Right PUSH Completed");
        StepVerifier.create(mono)
                    .expectNext(4L) //  총 4개가 되어야 한다.
                    .verifyComplete();
    }

    //@Test
    void givenLeftPush() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 왼쪽에서 데이터를 넣는다.
        Mono<Long> mono = reactiveListOps.leftPush("deliveryList", "delivery5")
                                         .log("All Right PUSH Completed");
        StepVerifier.create(mono)
                    .expectNext(5L) // 총 5개가 되어야 한다.
                    .verifyComplete();
    }

    //@Test
    void givenLeftPushAll() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 왼쪽에서 데이터를 넣는다.
        Mono<Long> mono = reactiveListOps.leftPushAll("deliveryList", Arrays.asList("delivery6", "delivery7", "delivery8"))
                                         .log("All Right PUSH Completed");
        StepVerifier.create(mono)
                    .expectNext(8L) // 총 8개가 되어야 한다.
                    .verifyComplete();
    }

    //@Test
    void givenLeftPop() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 왼쪽에서 데이터를 pop한다. pop한 데이터는 사라진다.
        Mono<String> mono = reactiveListOps.leftPop("deliveryList");
        StepVerifier.create(mono)
                    .assertNext(pop -> pop.equals("delivery8"))
                    .verifyComplete();
    }

    //@Test
    void givenRightPop() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 오른쪽에서 데이터를 pop한다. pop한 데이터는 사라진다.
        Mono<String> mono = reactiveListOps.rightPop("deliveryList");
        StepVerifier.create(mono)
                    .assertNext(pop -> pop.equals("delivery4"))
                    .verifyComplete();
    }

    //@Test
    void givenListExpiredSetting() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 4초 이후에 key : deliveryList에 있는 데이터가 사라진다.
        Mono<Long> mono = reactiveListOps.rightPushAll("deliveryList", Arrays.asList("delivery1", "delivery2", "delivery3"))
                                         .doOnNext(onNext -> redisTemplate.expire("deliveryList", Duration.ofSeconds(4)).subscribe())
                                         .log();
        StepVerifier.create(mono)
                    .expectNext(3L)
                    .verifyComplete();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 4초 이후 레디스로부터 데이터를 가져온다.
        Flux<String> flux = reactiveListOps.range("deliveryList", 0, 2)
                                           .log();

        StepVerifier.create(flux)
                    .expectNextCount(0) // expired에 의해 데이터가 삭제되었기 때문에 카운트 0
                    .verifyComplete();
    }

    //@Test
    void givenSetTest() {
        ReactiveSetOperations<String, String> reactiveSetOps = redisTemplate.opsForSet();
        //Mono<Long> mono = reactiveSetOps.add("set", "set1", "set2", "set3", "set3"); 중복 허용하지 않는 것 테스트
        // 위 테스트 진행시에는 "set3"가 중복이기 때문에 실제로는 3개의 데이터만 들어간다. 따라서 밑에 검증 단계에서 에러 발생
        Mono<Long> mono = reactiveSetOps.add("set", "set1", "set2", "set3", "set4");
        StepVerifier.create(mono)
                    .expectNext(4L)
                    .verifyComplete();
    }

    //@Test
    void givenSetMemberTest() {
        ReactiveSetOperations<String, String> reactiveSetOps = redisTemplate.opsForSet();
        Flux<String> flux = reactiveSetOps.members("set").filter(value -> value.equals("set2"));
        StepVerifier.create(flux)
                    .expectNext("set2")
                    .verifyComplete();
    }

    @Test
    void givenSetExpiredTest() {
        ReactiveSetOperations<String, String> reactiveSetOps = redisTemplate.opsForSet();
        Mono<Long> mono = reactiveSetOps.add("set", "set1", "set2", "set3", "set4")
                                        .doOnNext(onNext -> redisTemplate.expire("set", Duration.ofSeconds(4)).subscribe());
        StepVerifier.create(mono)
                    .expectNext(4L)
                    .verifyComplete();

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Flux<String> flux = reactiveSetOps.members("set");
        StepVerifier.create(flux)
                    .expectNextCount(0)
                    .verifyComplete();
    }
}
