package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.responses.CartResponse;
import com.haui.tech_shop.entities.Cart;

public interface CartService {
    Cart getCartById(Long id);
    CartResponse getCartResponse(Cart cart);
    Cart createCart(Cart cart);
    Cart findByCustomerId(Long customerId);

    Cart findById(Long cartId);
}
