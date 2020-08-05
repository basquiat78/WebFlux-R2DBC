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
