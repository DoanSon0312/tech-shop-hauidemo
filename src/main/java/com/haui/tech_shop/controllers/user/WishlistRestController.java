package com.haui.tech_shop.controllers.user;

import com.haui.tech_shop.entities.Wishlist;
import com.haui.tech_shop.services.interfaces.WishlistItemService;
import com.haui.tech_shop.services.interfaces.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.*;

@RestController
@RequestMapping("/user/wishlist")
@RequiredArgsConstructor
public class WishlistRestController {
    private final WishlistService wishlistService;
    private final WishlistItemService wishlistItemService;

    @PostMapping("/insert")
    public ResponseEntity<?> insertItemIntoWishlist(@RequestParam("wishlistId") Long wishlistId,
                                                    @RequestParam("productId") Long productId,
                                                    HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean inserted = wishlistItemService.insertItemIntoWishlist(wishlistId, productId);

            Wishlist wishlist = wishlistService.getWishlistById(wishlistId);
            int count = wishlist.getItems().size();

            // ✅ cập nhật session
            session.setAttribute("wishlistCount", count);

            response.put("wishlistCount", count);
            if (inserted) {
                response.put("success", true);
                response.put("message", "Added to Wishlist successfully!");
            } else {
                response.put("success", false);
                response.put("message", "Product already in Wishlist!");
            }
            return ResponseEntity.ok(response);

        } catch (NotFoundException ex) {
            response.put("success", false);
            response.put("message", "Wishlist or product not found!");
            return ResponseEntity.badRequest().body(response);
        }
    }


    @DeleteMapping("/remove")
    public ResponseEntity<?> removeItemFromWishlist(@RequestParam Map<String, String> params,
                                                    HttpSession session){
        try{
            Long wishlistId = Long.parseLong(params.get("wishlistId"));
            Long productId = Long.parseLong(params.get("productId"));
            wishlistItemService.removeItemFromWishlist(wishlistId, productId);
            int count = wishlistItemService.getItemsCount(wishlistId);
            // cập nhật session để Thymeleaf render lại đúng khi reload trang
            session.setAttribute("wishlistCount", count);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("wishlistCount", count);
            return ResponseEntity.ok(res);
        } catch (NotFoundException ex){
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không xoá được"));
        }
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearWishlist(@RequestParam Map<String, String> params,
                                           HttpSession session){
        try{
            Long wishlistId = Long.parseLong(params.get("wishlistId"));
            wishlistService.clearWishlist(wishlistId);
            // set 0 vào session
            session.setAttribute("wishlistCount", 0);
            return ResponseEntity.ok(Map.of("success", true, "wishlistCount", 0));
        } catch (NotFoundException ex){
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy wishlist"));
        }
    }


}
