# 혼자 북치고 장구치는 초간단 프로젝트
혼자서 북치고 장구치는 서버를 하나 만들어 볼까 한다.      

## Prerequisites     

1. OS: macOS Catalina v10.15.5
2. Java: OpenJDK(AdoptOpenJDK) 1.8.0.222
3. IDE: IntelliJ IDEA 2020.1.2 (Community Edition)
4. Plugin: Lombok
5. Database: PostgreSQL 12.3
6. Spring Boot: 2.3.2.RELEASE
7. Swagger: springfox-boot-starter 3.0.0
8. Redis: redis_version:6.0.6
9. Redis GUI Tool: Medis     

## Redis Installation
망할 Window10에서 Redis 설치는 좀 거시기한 것이 버전이 엄청 낮다. 거의 손 놓은 듯...            

하긴 왜 그걸 굳이 Window10에 깔아서 쓸까라는 생각이 퍼득 드는데 일단 Redis를 한번 설치해 보자.     

정말 간단하다.

일단 터미널을 연다.

```
brew update
brew install redis

/** 레디스 서버 시작 */
brew services start redis

/** 레디스 서버을 끄고 싶으면 밑에 명령어로 */
brew services stop redis
```
정말 간단하지 아니 할 수 없다.      

그리고 Redis는 기본적으로 in-memory DB로 사용할 수 있기 때문에 gui tool도 설치해 보자.       

예전에 Redis Desktop Manager, RDM이라고 설치해서 사용했었는데 이게 언제 유료화가 된겨????      

그래서 폭풍 검색으로 Medis라는 툴을 찾았다. gui는 확실히 RDM에 비해서 많이 모자르지만 충분히 GUI로 충분하다.      

참고로 nodeJs필수이다.     

설마... 설치 방법까지 알려줘야 하나? 좀 오래되긴 했지만 [nodejs installation](https://heropy.blog/2018/02/17/node-js-install/) 링크로...     

참고로 nvm으로 설치해서 버전 관리를 할 수 있게 하는 방식이 유리하다.      

깔았다는 전제하에 [medis 깃허브](https://github.com/luin/medis)에서 깃 주소를 복사하고 폴더 하나 생성해서 거기다가 작업을 해보자.

```
>cd <go to your directory>
>git clone https://github.com/luin/medis.git
>npm insatll
>npm run build
>npm start
```
시작하게 되면 Electron으로 하나의 툴을 띄우게 된다.     

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture1.png)    

비번 설정을 안했으니 그냥 connection을 누르면     

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture2.png)    

화면을 볼 수 있다.     

## Reactive Redis
그레이들 설정에 다음과 같이 추가를 한다.

```
implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
implementation group: 'org.apache.commons', name: 'commons-pool2', version: '2.8.1'
```
lettuce pool을 쓰기 위해선 commons-pool2가 필요하다.      

application.yml에 redis관련 설정을 넣어준다.

```
spring:
  profiles: local
  freemarker:
    charset: UTF-8
    check-template-location: true
    enabled: true
    suffix: .ftl
    template-loader-path: classpath:/templates
  r2dbc:
    url: r2dbc:pool:postgresql://127.0.0.1:5432/basquiat
    username: postgres
    password: basquiat
    pool:
      initial-size: 10
      max-size: 30
      max-idle-time: 30m
      validation-query: SELECT 1
  redis:
    host: localhost
    port: 6379
    password:
    lettuce:
      pool:
        min-idle: 2
        max-idle: 5
        max-active: 10
```
그냥 가장 기본적인 것들만 사용할 것이라면 만들지 않아도 auto-config 작동하게 되어있다.      

하지만 일단 어찌되었든 언젠가는 만들어야 하니 다음과 같이 클래스를 하나 만들자.

```
package io.basquiat.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@AllArgsConstructor
public class ReactiveRedisConfig {

    private final RedisConnectionFactory factory;

}

```
## 뭐가 되었든 Redis 한번 맛보자.      
예전에는 redis로 pub/sub과 spring session 공유를 위한 in-memory db로 사용했었다.      

그 외에도 cache를 위해서도 많이 사용하는데 그러면 어떻게 사용하는지에 대해서 한번 맛이라도 보자.     

### List타입의 PUSH

다음 테스트 코드를 한번 확인해 보자.

```
package io.basquiat.redis.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

    @Test
    void givenLeftPushAll() {
        ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
        // 왼쪽에서 데이터를 넣는다.
        Mono<Long> mono = reactiveListOps.leftPushAll("deliveryList", Arrays.asList("delivery6", "delivery7", "delivery8"))
                                         .log("All Right PUSH Completed");
        StepVerifier.create(mono)
                    .expectNext(8L) // 총 8개가 되어야 한다.
                    .verifyComplete();
    }

}
```

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture3.png)      

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture4.png)      

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture5.png)      

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture6.png)       

테스트 클래스에 어설프지만 그림으로 표현한 LPUSH, RPUSH에 따른 데이터가 들어가는 모습을 위의 이미지에서 유심히 살펴보기 바란다.     

index가 있어서 데이터가 들어간 구조를 생각하기 쉽다.      

그렇다면 POP과 관련된 테스트도 한번 진행해 보자.     

## List타입의 POP
Java의 queue와 stack에 대한 개념은 아마 다들 아실 것이다. LIFO(Last In First Out)과 FIFO(First In First Out)로 보통 설명한다.     

하지만 쉽게 얘기하면 그냥 물호수와 컵을 한번 생각해 보자.     

queue는 물호수에서 먼저 들어간 물이 가장 먼저 나오는 형태를 지니고 있다.         

그리고 stack은 네모난 컵을 생각하고 네모난 어떤 물질을 차곡 차곡 쌓으면 결국 먼저 들어간 녀석은 자기 위에 있는 다른 네모난 것들이 다 나가서야 나간다.     

그렇다면 우리가 앞서 LPUSH와 RPUSH로 들어갔던 데이터를 보면 결국 어떤 위치에서 데이터를 꺼내오냐에 따라 그 결과가 달라진다는 것을 알게 된다.     

긴말 할거 없이 코드로 보자.     

마지막 이미지에서 데이터가 들어가 있는 모습을 머리속에 그려가면서 한번 테스트 해보자.

```
@Test
void givenLeftPop() {
    ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
    // 왼쪽에서 데이터를 pop한다. pop한 데이터는 사라진다.
    Mono<String> mono = reactiveListOps.leftPop("deliveryList");
    StepVerifier.create(mono)
                .assertNext(pop -> pop.equals("delivery8"))
                .verifyComplete();
}
```      
맨 마지막 이미지대로라면 index가 0인 녀석의 값은 "delivery8"이다.      

검증이 완료되고 실제 medis에서 새로 고침으로 확인하면 LPOP이기 때문에 다음 이미지처럼 보여질 것이다.     

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture7.png)      

그러면 예상할 수 있듯이 RPOP을 하게 되면 "delivery4"가 지워질까?     

```
@Test
void givenRightPop() {
    ReactiveListOperations<String, String> reactiveListOps = redisTemplate.opsForList();
    // 오른쪽에서 데이터를 pop한다. pop한 데이터는 사라진다.
    Mono<String> mono = reactiveListOps.rightPop("deliveryList");
    StepVerifier.create(mono)
                .assertNext(pop -> pop.equals("delivery4"))
                .verifyComplete();
}
```

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture8.png)      

## List타입의 expired설정

expired는 유효기간을 설정하는 것이다.      

List타입의 경우에는 key에 대해서 expired를 설정하기 때문에 현재로써는 리스트 내의 각각의 element별로 줄 수는 없다.    

그럼 코드로 한번 살펴보자.

```
@Test
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
```
만일 캐시를 염두해 둔다면 이 expired를 통해 생명주기를 결정 할 수 있다.     

## Set
자바의 Set를 생각하면 쉽다.

1. value 중복 허용 안함     

2. 순서가 뭐에요? ~~먹는건가요?~~     

```
@Test
void givenSetTest() {
    ReactiveSetOperations<String, String> reactiveSetOps = redisTemplate.opsForSet();
    //Mono<Long> mono = reactiveSetOps.add("set", "set1", "set2", "set3", "set3"); 중복 허용하지 않는 것 테스트
    // 위 테스트 진행시에는 "set3"가 중복이기 때문에 실제로는 3개의 데이터만 들어간다. 따라서 밑에 검증 단계에서 에러 발생
    Mono<Long> mono = reactiveSetOps.add("set", "set1", "set2", "set3", "set4");
    StepVerifier.create(mono)
                .expectNext(4L)
                .verifyComplete();
}
```
![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture9.png)      

"순서는 내 사전에 없다. 그냥 막 넣으면 그만이다!" - Set의 어록      

특이한 점은 GUI를 보면 value가 아니라 member라고 표현하는 것을 알 수 있다.

```
@Test
void givenSetMemberTest() {
    ReactiveSetOperations<String, String> reactiveSetOps = redisTemplate.opsForSet();
    Flux<String> flux = reactiveSetOps.members("set").filter(value -> value.equals("set2"));
    StepVerifier.create(flux)
                .expectNext("set2")
                .verifyComplete();
}
```
members라는 메소드를 통해서 해당 키의 정보를 가져온다.      

그러면 이넘도 expired가 가능한가?

앞서 테스트도 그렇고 key는 중복 허용을 하지 않기 때문에 GUI에서 기존의 데이터를 삭제하고 테스트한다.

```
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
```
이외에도 Hash, SortedSet을 지원하는 ZSet이나 좌표 정보로 처리하는 Geo라든가 HyperLogLog같은 것도 지원한다.         

하지만 개인적으로 다뤄본 종류는 그나마 hash나 zSet정도인데 실무에서 사용해 본 경험이 없다.      

다만 사용법은 거의 비슷하기 때문에 필요에 의해서 사용법을 확인해 보면 될것 같다.      

## Redis Publisher/Subscriber (pub/sub)     
Message Queue와 관련해서 kafka, RabbitMQ같은 녀석들이 있지만 일단 우리는 '혼자서 북치고 장구'를 쳐야 하니 Redis를 통해 pub/sub 구현을 한다.     

앞서 빈 껍데기만 만들어 놓은 ReactiveRedisConfig에 설정 코드를 추가한다.

```
package io.basquiat.config;

import io.basquiat.delivery.model.Delivery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class ReactiveRedisConfig {

    /**
     * topic channel open
     * @return ChannelTopic
     */
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("delivery");
    }

    /**
     * ReactiveRedisMessageListenerContainer regist
     * @param factory
     * @return ReactiveRedisMessageListenerContainer
     */
    @Bean
    public ReactiveRedisMessageListenerContainer messageListenerContainer(ReactiveRedisConnectionFactory factory) {
        // ReactiveRedisMessageListenerContainer 생성
        ReactiveRedisMessageListenerContainer redisMessageListenerContainer = new ReactiveRedisMessageListenerContainer(factory);
        // container에 "delivery:queue" 채널 토픽으로 들어오는 메세지를 받는 부분을 등록한다.
        redisMessageListenerContainer.receive(topic());
        return redisMessageListenerContainer;
    }

    /**
     * ReactiveRedisOperations<String, Delivery> Setup and regist
     * @param factory
     * @return
     */
    @Bean
    public ReactiveRedisOperations<String, Delivery> redisOperations(ReactiveRedisConnectionFactory factory) {
        // 일반적인 ReactiveRedisOperations 빈 등록 방식
        Jackson2JsonRedisSerializer<Delivery> serializer = new Jackson2JsonRedisSerializer<>(Delivery.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Delivery> builder =
                            RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Delivery> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

}
```

Delivery라는 도메인을 하나 만들자.

```
package io.basquiat.delivery.model;

import lombok.*;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@ToString
public class Delivery implements Serializable {

    @Builder
    public Delivery(String ticketId, String zipCode, String city, String street) {
        this.ticketId = ticketId;
        this.zipCode = zipCode;
        this.city = city;
        this.street = street;
    }

    private String ticketId;

    private String zipCode;

    private String city;

    private String street;

}
```

자 그럼 문득 왜 이딴 걸 하는 걸까라는 의문이 든다.      

이 브랜치는 앞에서 크게 적어논 '혼자서 북치고 장구치는' 프로젝트이다.      

말이 안되는 시나리오지만 이런 기능을 토대로 시나리오 중심으로 SSE를 한번 구현해 볼 생각이다.      

## Server Side Event Or Server Sent Event (SSE)            

예전에 흔적을 남겼던 나의 [sever-sent-event](https://github.com/basquiat78/sever-sent-event)를 참고하면 될 것 같다.      

하지만 일단 이것이 뭔지 코드로 직접 구현해서 한번 살펴보자.

그 전에 우리는 FreeMarker를 사용하고 있다.      

그래서 WebConfig를 하나 만들 것이다.

```
package io.basquiat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;

/**
 * created by basquiat
 */
@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    /**
     * cors setup
     * @param registry
     */
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*");
    }

    /**
     * freemarker view resolver
     * @param registry
     */
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
    }

    /**
     * freemarker config
     * @return FreeMarkerConfigurer
     */
    @Bean
    public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer freeMakerConfig = new FreeMarkerConfigurer();
        freeMakerConfig.setTemplateLoaderPath("classpath:/templates");
        return freeMakerConfig;
    }

}
```
그리고 applicaion.yml에도 설정을 해줘야 한다.

```
spring:
  profiles: local
  freemarker:
    charset: UTF-8
    check-template-location: true
    enabled: true
    suffix: .ftl
    template-loader-path: classpath:/templates
  r2dbc:
    url: r2dbc:pool:postgresql://127.0.0.1:5432/basquiat
    username: postgres
    password: basquiat
    pool:
      initial-size: 10
      max-size: 30
      max-idle-time: 30m
      validation-query: SELECT 1
  redis:
    host: localhost
    port: 6379
    password:
    lettuce:
      pool:
        min-idle: 2
        max-idle: 5
        max-active: 10
```
이렇게 설정을 해줘야 freemarker로 작성된 파일을 접근할 수 있다.      

그러면 이제 우리는 viewController를 작성하자.

```
package io.basquiat.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *
 * View Controller
 *
 * created by basquiat
 *
 */
@Controller
public class ViewController {

    /**
     * index html render controller
     * @param model
     * @return String
     */
    @GetMapping("/")
    public String index(final Model model) {
        return "index";
    }

}
```
설정이 완료되었으니 

```
freeMakerConfig.setTemplateLoaderPath("classpath:/templates");
```
코드에서 처럼 resource폴더 밑에 templates폴더를 생성하고 index.ftl을 하나 만들자.

```
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
	<div class="container">
	    <div>
	        <div id="title">
	            <h1>Spring WebFlux Server Sent-Events</h1>
	        </div>
	        <div id="sse"></div>
	    </div>
	</div>
</body>

<script>
    let source;
    function loadScript () {
        source = new EventSource("http://localhost:8080/delivery/event");
    }

    function start() {
        source.onmessage = event => {
            let data = event.data;
            let div = document.getElementById('sse');
            div.innerHTML += "<div> Server Sent-Event Info : " + data + "</div>";
        };

        source.onerror = () => {
            this.close();
        };
        source.stop = () => {
            this.source.close();
        };

    }

    window.onload = () => {
        loadScript();
        start();
    };
</script>

</html>
``` 
정말 불품없지만 앞으로 하나씩 붙여나간다는 생각으로 시작한다.       

그리고 DeliveryController를 하나를 만들것이다.

```$xslt
package io.basquiat.delivery.web;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.time.Duration;

/**
 *
 * Server Sent-Event Controller
 *
 * created by basquiat
 *
 */
@RestController
@RequestMapping("/delivery")
@AllArgsConstructor
@Slf4j
public class DeliveryController {
    
    /**
     * 10초마다 정보를 반환한다.
     * @return Flux<String>
     */
    @GetMapping(path = "/event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamFlux() {
        log.info("Server Sent Event Ready!");
        return Flux.interval(Duration.ofSeconds(10))
                   .flatMap(sequence -> Flux.just("test_" + sequence));
    }

}
```
코드는 10초마다 스트링 값을 클라이언트로 보내는 설정이다. 이때 produces = MediaType.TEXT_EVENT_STREAM_VALUE 옵션을 설정해 준다.      

ViewController에서 딱히 경로를 주지 않았기 때문에 [View](http://localhost:8080/)로 접속해 보자.    

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture10.png)       

이미지처럼 10초마다 값을 찍는 것을 확인할 수 있다.      

자 그러면 SSE가 뭔지 살짝 감이 올 것이다. 웹소켓과는 다르긴 하지만 실제로 나는 이것을 푸시 알람으로 사용했었다.      

하지만 이것은 뭔가 현실적이지 않다.     

10초마다라는 제약이 있는데 그러면 어떤 이벤트가 들어올 때, 즉 그러니깐 레디스를 통해서 publish 이벤트가 발생하면 subscribe해서 값을 던져줄 수 없을까?           

당연히 있다.     

예전에는 푸시알람 구현시에 SSEEmitter를 구현하는 방식으로 복잡하게 꼬아서 작업했는데 단지 리스너하나만 구현해서 쉽게 사용할 수 있다.      

우리는 ReactiveRedisConfig클래스에서 ReactiveRedisMessageListenerContainer을 @Bean으로 등록을 했는데 이제부터 이것을 사용할 것이다.     

## 시나리오     
현재 이 프로젝트의 시나리오는 다음과 같다.      

1. 점주분이 라이더에게 배달 요청을 보낸다. 배달 정보는 city, street, zipCode를 담고 있는 Delivery클래스를 참조하며 ticketId는 서버에서 UUID로 생성한다.    

2. 요청을 받은 컨트롤로는 해당 정보를 redis에 담고 ReactiveRedisConfig에서 설정한 토픽채널 delivery로 ticketId와 함께 publish한다.      

3. SSE가 이 메세지를 받으면 레디스에서 넘어온 ticketId로 레디스에서 정보를 가져 와서 화면에 정보를 뿌린다.       

자 그럼 이제부터 Service를 하나 만들어 보자.

DeliveryService

```
package io.basquiat.delivery.service;

import io.basquiat.delivery.model.Delivery;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service("deliveryService")
@AllArgsConstructor
public class DeliveryService {

    /** ReactiveRedisConfig에서 등록한 빈들을 주입 */
    private final ReactiveRedisOperations<String, Delivery> deliveryOps;

    private final ReactiveRedisTemplate<String, String> reactiveTemplate;

    private final ReactiveRedisMessageListenerContainer reactiveMsgListenerContainer;

    private final ChannelTopic topic;

    /**
     * 배달 요청을 처리하는 서비스
     * @param delivery
     * @return Mono<Delivery>
     */
    public Mono<Delivery> orderDelivery(Delivery delivery) {
        /*
            1. UUID로 ticketId발급
            2. ticketId를 키로 delivery정보를 레디스에 넣는다. expired는 10초
            3. 그 다음에 reactiveTemplate를 통해 토픽 채널로 ticketId를 담아 publish한다.
            4. 그리고 ticketId가 세팅된 delivery객체를 다시 반환한다.
         */
        delivery.setTicketId(UUID.randomUUID().toString());
        return deliveryOps.opsForValue().set(delivery.getTicketId(), delivery, Duration.ofSeconds(10))
                          .doOnNext(next -> reactiveTemplate.convertAndSend(topic.getTopic(), delivery.getTicketId()).subscribe())
                          .then(Mono.just(delivery));
    }

    /**
     * reactiveMsgListenerContainer에 등록된 채널로 publish event가 발생하면 subscribe하다가 이벤트 감지하는 서비스
     * @return Flux<Delivery>
     */
    public Flux<Delivery> deliveryListner() {
        /*
            1. topic의 채널을 subscribe하고 있다.
            2. ReactiveSubscription객체로부터 넘어온 메세지를 가져온다.
            3. ticketId가 넘어오면 그 키로 레디스에서 해당 정보를 가져와 반환한다.
         */
        return reactiveMsgListenerContainer.receive(topic)
                                           .map(ReactiveSubscription.Message::getMessage)
                                           .flatMap(key -> Flux.from(deliveryOps.opsForValue().get(key)));
    }

}
```
위에 각 메소드마다 주석으로 설명을 좀 세세하게 달아놨다.      

이제 DeliveryServiceTest코드를 작성한다.

```
package io.basquiat.delivery.test;

import io.basquiat.delivery.model.Delivery;
import io.basquiat.delivery.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DeliveryServiceTest {

    @Autowired
    private DeliveryService deliveryService;

    @Test
    void givenOrderDelivery() {
        Delivery delivery = Delivery.builder().city("서울").street("논현동").zipCode("5000").build();
        deliveryService.orderDelivery(delivery).subscribe(System.out::println);
    }

}
```
테스트 전에 해야 할 일은 다음과 같다.     

1. medis에서 terminal을 클릭한다.      

2. 터미널에서 subscribe delivery을 입력한다. 채널 설정이 delivery이니 이 채널을 구독하는 것이다.     

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture11.png)       

이미지처럼 subscribe 모드로 돌입한 것을 알 수 있다.      

그리고 테스트를 실행하게 되면 

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture12.png)      

이미지처럼 보내진 메세지 이벤트를 받아서 출력하고 그 티켓 아이디로 정보가 생성된 것을 볼 수 있다.           

사실 SSE Controller레벨 테스트하는 방법을 찾지 못해서 바로 나머지 서비스와 컨트롤러를 작성해서 실제로 테스트를 해보자.     

지금까지의 DelvierySevice는 다음과 같다.

```
package io.basquiat.delivery.service;

import io.basquiat.delivery.model.Delivery;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service("deliveryService")
@AllArgsConstructor
public class DeliveryService {

    /** ReactiveRedisConfig에서 등록한 빈들을 주입 */
    private final ReactiveRedisOperations<String, Delivery> deliveryOps;

    private final ReactiveRedisTemplate<String, String> reactiveTemplate;

    private final ReactiveRedisMessageListenerContainer reactiveMsgListenerContainer;

    private final ChannelTopic topic;

    /**
     * 배달 요청을 처리하는 서비스
     * @param delivery
     * @return Mono<Delivery>
     */
    public Mono<Delivery> orderDelivery(Delivery delivery) {
        /*
            1. UUID로 ticketId발급
            2. ticketId를 키로 delivery정보를 레디스에 넣는다. expired는 10초
            3. 그 다음에 reactiveTemplate를 통해 토픽 채널로 ticketId를 담아 publish한다.
            4. 그리고 ticketId가 세팅된 delivery객체를 다시 반환한다.
         */
        delivery.setTicketId(UUID.randomUUID().toString());
        return deliveryOps.opsForValue().set(delivery.getTicketId(), delivery, Duration.ofSeconds(10))
                          .doOnNext(next -> reactiveTemplate.convertAndSend(topic.getTopic(), delivery.getTicketId()).subscribe())
                          .then(Mono.just(delivery));
    }

    /**
     * reactiveMsgListenerContainer에 등록된 채널로 publish event가 발생하면 subscribe하다가 이벤트 감지하는 서비스
     * @return Flux<Delivery>
     */
    public Flux<Delivery> deliveryListner() {
        /*
            1. topic의 채널을 subscribe하고 있다.
            2. ReactiveSubscription객체로부터 넘어온 메세지를 가져온다.
            3. ticketId가 넘어오면 그 키로 레디스에서 해당 정보를 가져와 반환한다.
         */
        return reactiveMsgListenerContainer.receive(topic)
                                           .map(ReactiveSubscription.Message::getMessage)
                                           .flatMap(key -> Flux.from(deliveryOps.opsForValue().get(key)));
    }

}
```     
시나리오대로 요청을 받으면 레디스에 발급한 티켓아이디를 키값으로 정보를 저장하고 해당 키를 메제시지로 delivery채널에 publish가 있다.       

그리고 하나는 리스너로 delivery채널에 pub event가 발생하면 key를 받아서 레디스에서 해당 키에 대한 delivery정보를 받아온다.      

그리고 DeliveryController은 다음과 같이 작성한다. 

```
package io.basquiat.delivery.web;

import io.basquiat.delivery.model.Delivery;
import io.basquiat.delivery.service.DeliveryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * Server Sent-Event Controller
 *
 * created by basquiat
 *
 */
@RestController
@RequestMapping("/delivery")
@AllArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("")
    public Mono<Delivery> orderDelivery(@RequestBody Delivery delivery) {
        return deliveryService.orderDelivery(delivery);
    }

    @GetMapping(path = "/event/{street}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Delivery> streamFlux(@PathVariable("street") String street) {
        log.info("Server Side Event Ready!" + street);
        return deliveryService.deliveryListner();
    }

}
```
SSE에서 PathVariable로 변수 하나를 받는 것이 눈에 보이는데 이것은 다음과 같이 사용하기 위해서이다.      

ViewController 수정

```
package io.basquiat.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 *
 * View Controller
 *
 * created by basquiat
 *
 */
@Controller
public class ViewController {

    /**
     * index html render controller
     * @param model
     * @return String
     */
    @GetMapping("/view/{street}")
    public String index(@PathVariable("street") String street, final Model model) {
        model.addAttribute("street", street);
        return "index";
    }

}
```

라이더는 자신의 위치에 맞는 정보만 받아야 하기 때문에 index.ftl을 좀 수정하자.

```
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
	<div class="container">
	    <div>
	        <div id="title">
	            <h1>Spring WebFlux Server Sent-Events</h1>
	        </div>
	        <div id="sse"></div>
	    </div>
	</div>
</body>

<script>
    let source;
    let path = '${street}';
    console.log(path)
    function loadScript () {
        source = new EventSource("http://localhost:8080/delivery/event/"+path);
    }

    function start() {
        source.onmessage = event => {
            let data = event.data;
            let div = document.getElementById('sse');
            div.innerHTML += "<div> Server Sent-Event Info : " + data + "</div>";
        };

        source.onerror = () => {
            this.close();
        };
        source.stop = () => {
            this.source.close();
        };

    }

    window.onload = () => {
        loadScript();
        start();
    };
</script>

</html>
```
당연히 DeliveryController, DeliveryService도 수정해야 한다.

최종 DeliveryController

```
package io.basquiat.delivery.web;

import io.basquiat.delivery.model.Delivery;
import io.basquiat.delivery.service.DeliveryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * Server Sent-Event Controller
 *
 * created by basquiat
 *
 */
@RestController
@RequestMapping("/delivery")
@AllArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("")
    public Mono<Delivery> orderDelivery(@RequestBody Delivery delivery) {
        return deliveryService.orderDelivery(delivery);
    }

    @GetMapping(path = "/event/{street}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Delivery> streamFlux(@PathVariable("street") String street) {
        log.info("Server Side Event Ready!" + street);
        return deliveryService.deliveryListner(street);
    }

}
```

최종 DeliveryService

```
package io.basquiat.delivery.service;

import io.basquiat.delivery.model.Delivery;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service("deliveryService")
@AllArgsConstructor
public class DeliveryService {

    /** ReactiveRedisConfig에서 등록한 빈들을 주입 */
    private final ReactiveRedisOperations<String, Delivery> deliveryOps;

    private final ReactiveRedisTemplate<String, String> reactiveTemplate;

    private final ReactiveRedisMessageListenerContainer reactiveMsgListenerContainer;

    private final ChannelTopic topic;

    /**
     * 배달 요청을 처리하는 서비스
     * @param delivery
     * @return Mono<Delivery>
     */
    public Mono<Delivery> orderDelivery(Delivery delivery) {
        /*
            1. UUID로 ticketId발급
            2. ticketId를 키로 delivery정보를 레디스에 넣는다. expired는 10초
            3. 그 다음에 reactiveTemplate를 통해 토픽 채널로 ticketId를 담아 publish한다.
            4. 그리고 ticketId가 세팅된 delivery객체를 다시 반환한다.
         */
        delivery.setTicketId(UUID.randomUUID().toString());
        return deliveryOps.opsForValue().set(delivery.getTicketId(), delivery, Duration.ofSeconds(10))
                          .doOnNext(next -> reactiveTemplate.convertAndSend(topic.getTopic(), delivery.getTicketId()).subscribe())
                          .then(Mono.just(delivery));
    }

    /**
     * reactiveMsgListenerContainer에 등록된 채널로 publish event가 발생하면 subscribe하다가 이벤트 감지하는 서비스
     * @return Flux<Delivery>
     */
    public Flux<Delivery> deliveryListner(String street) {
        /*
            1. topic의 채널을 subscribe하고 있다.
            2. ReactiveSubscription객체로부터 넘어온 메세지를 가져온다.
            3. ticketId가 넘어오면 그 키로 레디스에서 해당 정보를 가져온다.
            4. 라이더가 위치한 street의 delivery정보인지 추려낸다.
         */
        return reactiveMsgListenerContainer.receive(topic)
                                           .map(ReactiveSubscription.Message::getMessage)
                                           .flatMap(key -> Flux.from(
                                                                        deliveryOps.opsForValue()
                                                                                   .get(key)
                                                                                   .filter(d -> d.getStreet().equals(street))
                                                                     )
                                            );
    }

}
```

자 이제 실행을 하고 라이더는 자신의 위치에 따라서 다음과 같이 접속을 한다.

[논현1동에 있는 라이더](http://localhost:8080/view/논현1동)     

[청담동에 있는 라이더](http://localhost:8080/view/청담동)     

[Swagger에서 테스트하자](http://localhost:8080/documentation/swagger-ui/#/delivery-controller/orderDeliveryUsingPOST)     

스웨거로 들어가서 다음과 

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture13.png)      

처럼 street정보를 바꿔가면서 테스트를 막 눌러보자.      

postman으로 테스트해도 무방하다.       

스크립트로 연속적인 요청을 원하면 스크립트로 curl로 보내도 된다.       

![실행이미지](https://github.com/basquiat78/WebFlux-R2DBC/blob/1.혼자서북치고장구치기/capture/capture14.png)      

이미지처럼 필터링을 통해서 자신이 접속한 지역의 배달 요청 정보만 받을 수 있게 되었다.       