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
