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
