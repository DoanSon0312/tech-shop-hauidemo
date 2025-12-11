package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.dtos.responses.CartResponse;
import com.haui.tech_shop.entities.Cart;
import com.haui.tech_shop.entities.CartDetail;
import com.haui.tech_shop.repositories.CartRepository;
import com.haui.tech_shop.services.interfaces.CartService;
import com.haui.tech_shop.services.interfaces.ICartDetailService;
import com.haui.tech_shop.utils.Constant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;

    @Autowired
    @Lazy
    private ICartDetailService cartDetailService;

    @Override
    public Cart getCartById(Long id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }

    @Override
    public CartResponse getCartResponse(Cart cart) {
        // Lấy danh sách chi tiết sản phẩm trong giỏ
        List<CartDetail> cartDetails = cartDetailService.findAllByCart_Id(cart.getId());

        // Tính tổng tiền dựa trên quantity và price của từng sản phẩm
        BigDecimal totalPrice = cartDetails.stream()
                .map(cd -> cd.getProduct().getPrice().multiply(new BigDecimal(cd.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Trả về CartResponse với số tiền đã format
        return CartResponse.builder()
                .totalPrice(Constant.formatter.format(totalPrice))
                .build();
    }

    @Override
    public Cart createCart(Cart cart) {
        try {
            cartRepository.save(cart);
            return cart;
        }
        catch (Exception e) {
            throw new RuntimeException("Cart creation failed");
        }
    }

    @Override
    public Cart findByCustomerId(Long customerId) {
        return cartRepository.findByUserId(customerId).orElse(null);
    }

    @Override
    public Cart findById(Long cartId){
        return cartRepository.findById(cartId).orElse(null);
    }

}
