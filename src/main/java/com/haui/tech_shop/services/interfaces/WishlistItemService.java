package com.haui.tech_shop.services.interfaces;

import com.haui.tech_shop.dtos.responses.WishlistItemResponse;
import com.haui.tech_shop.entities.Wishlist;
import com.haui.tech_shop.entities.WishlistItem;

import java.util.List;

public interface WishlistItemService {
    Wishlist getWishlist(Long id);
    WishlistItem getItem(Long wishlistId, Long productId);
    List<WishlistItemResponse> getItems(Long wishlistId);
    boolean insertItemIntoWishlist(Long wishlistId,Long productId);
    void removeItemFromWishlist(Long wishlistId,Long productId);
    int getItemsCount(Long wishlistId);
}
