package io.basquiat.user.test;

import io.basquiat.user.model.User;
import io.basquiat.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserServiceTest {

    @Autowired
    private UserService userService;

    //@Test
    void givenInsertUser() {
        User newUser = User.builder().name("사이먼 도미닉").age(36).build();
        Mono<User> mono = userService.createUser(newUser);
        StepVerifier.create(mono)
                    .assertNext(user-> assertThat(user.getId()).isEqualTo(1L))
                    .verifyComplete();
    }

    //@Test
    void givenCustomInsertUser() {
        User newUser = User.builder().name("Simon Dominic").age(36).build();
        Mono<User> mono = userService.customCreateUser(newUser);
        StepVerifier.create(mono)
                    .assertNext(user-> assertThat(user.getId()).isEqualTo(2L))
                    .verifyComplete();
    }

    //@Test
    void givenFindAll() {
        Flux<User> flux = userService.findAll();
        StepVerifier.create(flux)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("사이먼 도미닉"))
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("Simon Dominic"))
                    .verifyComplete();
    }

    //@Test
    void givenCustomFindAll() {
        Flux<User> flux = userService.customFindAll();
        StepVerifier.create(flux)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("사이먼 도미닉"))
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("Simon Dominic"))
                    .verifyComplete();
    }

    //@Test
    void givenCustomFindAllProjection() {
        Flux<User> flux = userService.customFindAllProjection();
        StepVerifier.create(flux)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("사이먼 도미닉"))
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("Simon Dominic"))
                    .verifyComplete();
    }

    //@Test
    void givenUpdateUser() {
        User updateUser = User.builder().id(1L).name("쌈디").age(36).build();
        Mono<User> mono = userService.updateUser(updateUser);
        StepVerifier.create(mono)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("쌈디"))
                    .verifyComplete();
    }

    //@Test
    void givenCustomUpdateUser() {
        User updateUser = User.builder().id(1L).name("정기석").age(36).build();
        Mono<User> mono = userService.customUpdateUser(updateUser);
        StepVerifier.create(mono)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("정기석"))
                    .verifyComplete();
    }

    //@Test
    void givenCustomDeleteUser() {
        Mono<Void> mono = userService.customDeleteUser(2L);
        StepVerifier.create(mono)
                    .verifyComplete();
    }

    //@Test
    void givenFindByNameAndAge() {
        Flux<User> flux = userService.findByNameAndAge("이센스", 33);
        StepVerifier.create(flux)
                    .assertNext(user-> assertThat(user.getName()).isEqualTo("이센스"))
                    .verifyComplete();
    }

    @Test
    void givenFindByNameContaining() {
        Flux<User> flux = userService.findByNameContaining("센스");
        StepVerifier.create(flux)
                    .expectNextCount(2)
                    .verifyComplete();
    }

}
