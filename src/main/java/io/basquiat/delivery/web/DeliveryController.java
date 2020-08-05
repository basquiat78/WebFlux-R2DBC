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