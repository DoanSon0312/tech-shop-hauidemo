package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.requests.CartDetailRequest;
import com.haui.tech_shop.dtos.responses.CartDetailResponse;
import com.haui.tech_shop.dtos.responses.WishlistItemResponse;
import com.haui.tech_shop.entities.CartDetail;

import java.util.List;

public interface ICartDetailService {
    List<CartDetail> findAllByCart_Id(Long id);
    List<CartDetailResponse> getAllItems(List<CartDetail> cartDetails);

    CartDetailResponse convertToCartDetailReponse(CartDetail cartDetail);

    List<WishlistItemResponse> getAllWishlist();
    boolean create(CartDetailRequest cartDetailRequest);
    boolean update(CartDetailRequest cartDetailRequest);

    boolean delete(CartDetail cart);

    boolean deleteAll(Long cartId);

    CartDetail findByCart_IdAndProductId(Long id, Long productId);
}
