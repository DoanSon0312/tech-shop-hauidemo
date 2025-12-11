package com.haui.tech_shop.dtos.responses;

import com.haui.tech_shop.entities.Address;
import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.enums.OrderStatus;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderReponse {
    private Long orderId;
    private User shipper;
    private OrderStatus status;
    private String customerName;
    private String paymentName;
    private String totalPrice;
    private Address shippingAddress;
}