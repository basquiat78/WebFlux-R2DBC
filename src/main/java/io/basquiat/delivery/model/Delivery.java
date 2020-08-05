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
