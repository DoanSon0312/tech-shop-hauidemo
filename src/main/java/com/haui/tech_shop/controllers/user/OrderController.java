package com.haui.tech_shop.controllers.user;

import com.haui.tech_shop.dtos.requests.UserRequest;
import com.haui.tech_shop.dtos.responses.CartDetailResponse;
import com.haui.tech_shop.entities.Cart;
import com.haui.tech_shop.entities.Order;
import com.haui.tech_shop.entities.User;
import com.haui.tech_shop.enums.OrderStatus;
import com.haui.tech_shop.services.interfaces.CartService;
import com.haui.tech_shop.services.interfaces.ICartDetailService;
import com.haui.tech_shop.services.interfaces.IOrderService;
import com.haui.tech_shop.services.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/user/orders")
@RequiredArgsConstructor
public class OrderController {
    private final IOrderService orderService;
    private final UserService userService;
    private final CartService cartService;
    private final ICartDetailService cartDetailService;

    public UserRequest getUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        UserRequest userRequest = UserRequest.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .dob(user.getDateOfBirth())
                .active(user.isActive())
                .image(user.getImage())
                .build();
        return userRequest;
    }

    @GetMapping("/detail")
    public String detail(Model model, @RequestParam Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Cart cart = new Cart();

        List<CartDetailResponse> cartDetailListFull = new ArrayList<>();
        int numberProductInCart = 0;

        if(!username.equals("anonymousUser")) {
            UserRequest userRequest = getUser();
            cart = cartService.findByCustomerId(userRequest.getId());
            cartDetailListFull = cartDetailService.getAllItems(cartDetailService.findAllByCart_Id(cart.getId()));
            numberProductInCart = cartDetailListFull.size();
            if(cartDetailListFull.size() > 3) {
                cartDetailListFull = cartDetailListFull.subList(0, 3);
            }
            model.addAttribute("isEmptyCart", cartDetailListFull.isEmpty());
        }

        Order order = orderService.findById(id).get();

        User user = userService.getUserByUsername(username);
        model.addAttribute("cart",cart);
        model.addAttribute("cartDetailList", cartDetailListFull);
        model.addAttribute("numberProductInCart", numberProductInCart);
        model.addAttribute("totalPriceOfCart",cartService.getCartResponse(cart));

        model.addAttribute("orderStatus", OrderStatus.values());
        model.addAttribute("order", order);
        model.addAttribute("paymentMethod", order.getPayment().getName());
        return "user/order-detail";
    }

    @PostMapping("/cancelled")
    public String cancel(Model model, @RequestParam Long id) throws IOException {
        orderService.orderCancelled(id);
        return "redirect:/user/my-account";
    }

    @PostMapping("/completed")
    public String completedOrder(Model model, @RequestParam Long id) {
        orderService.orderCompleted(id);
        model.addAttribute("msg", "Đã hoàn thành đơn hàng");
        return "redirect:/user/my-account";
    }

    @PostMapping("/refund")
    public String refundOrder(Model model, @RequestParam Long id) {
        User user = userService.getUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        orderService.orderRefund(id);
        orderService.orderCompleted(id);
        model.addAttribute("msg", "Đã hoàn tiền cho đơn hàng");
        model.addAttribute("balanceVND", user.getBalance());
        return "user/wallet";
    }
}
