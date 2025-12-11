package com.haui.tech_shop.services.Impl;

import com.haui.tech_shop.dtos.responses.CartDetailResponse;
import com.haui.tech_shop.dtos.responses.OrderReponse;
import com.haui.tech_shop.entities.*;
import com.haui.tech_shop.repositories.*;
import com.haui.tech_shop.services.interfaces.*;
import com.haui.tech_shop.enums.OrderStatus;
import com.haui.tech_shop.utils.Constant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {
    private final OrderRepository orderRepository;
    private final IProductService productService;
    private final ICartDetailService cartDetailService;
    private final IVoucherService voucherService;
    private final UserService userService;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartDetailRepository cartDetailRepository;

    @Override
    public List<Order> findByUsername(String username) {
        return orderRepository.findByUser_Username(username);
    }

    @Override
    public void orderPending(Long id) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
    }

    @Override
    public void orderCancelled(Long id) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    @Override
    public void orderDelivered(Long id) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    @Override
    public void orderShipping(Long id, Long shipperId) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.SHIPPING);
        order.setShipper(userService.getUser(shipperId));
        orderRepository.save(order);
    }

    @Override
    public void orderRefund(Long id) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.REFUND);
        User user = order.getUser();
        user.setBalance(Optional.ofNullable(user.getBalance()).orElse(BigDecimal.ZERO).add(order.getTotalPrice()));
        userRepository.save(user);
        orderRepository.save(order);
    }

    @Override
    public void orderCompleted(Long id) {
        Order order = orderRepository.findById(id).get();
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    @Override
    public void createOrder(User user, BigDecimal totalPrice, Voucher voucher, Payment payment, Address address,
                            Long cartId,
                            List<CartDetailResponse> cartDetailList) {
        Order order = new Order();
        order.setTotalPrice(totalPrice);
        order.setActive(true);
        order.setUser(user);
        order.setVoucher(voucher);
        // Decrease voucher quantity
        voucherService.decreaseQuantity(voucher.getId());
        order.setPayment(payment);
        order.setAddress(address);
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (CartDetailResponse cartDetailResponse : cartDetailList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setProduct(productService.findByName(cartDetailResponse.getProductName()));
            orderDetail.setQuantity(cartDetailResponse.getQuantity());
            // Decrease product quantity
            productService.decreaseStockQuantity(cartDetailResponse.getProductId(), cartDetailResponse.getQuantity());
            orderDetail.setTotalPrice(cartDetailResponse.getTotalPrice());
            orderDetails.add(orderDetail);
            CartDetail cartDetail = cartDetailService.findByCart_IdAndProductId(cartId, cartDetailResponse.getProductId());
            cartDetailService.delete(cartDetail);
        }
        order.setOrderDetails(orderDetails);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order createOrder(User user, BigDecimal totalPrice, Payment payment,
                             Address address, Long cartId,
                             List<CartDetailResponse> cartDetailList) {

        // Validate dữ liệu
        validateOrderData(user, totalPrice, address, cartDetailList);

        Order order = new Order();
        order.setTotalPrice(totalPrice);
        order.setActive(true);
        order.setUser(user);
        order.setPayment(payment);
        order.setAddress(address);

        // Tạo order details
        List<OrderDetail> orderDetails = createOrderDetails(order, cartId, cartDetailList);
        order.setOrderDetails(orderDetails);

        return orderRepository.save(order); // Trả về order đã được lưu
    }

    @Override
    public void deleteAll() {
        orderRepository.deleteAll();
    }

    @Override
    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public <S extends Order> S save(S entity) {
        return orderRepository.save(entity);
    }

    @Override
    public Optional<Order> findById(Long aLong) {
        return orderRepository.findById(aLong);
    }

    @Override
    public boolean existsById(Long aLong) {
        return orderRepository.existsById(aLong);
    }

    @Override
    public long count() {
        return orderRepository.count();
    }

    @Override
    public boolean deleteById(Long aLong) {
        try {
            if (orderRepository.existsById(aLong)) {
                orderRepository.deleteById(aLong);
                return true;
            } else {

                return false;
            }
        } catch (Exception e) {
            // Log the error for debugging (e.g., constraint violations)
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Order> findAll(Sort sort) {
        return orderRepository.findAll(sort);
    }

    @Override
    public List<OrderReponse> findOrderByShipperNameAndStatus(Long shipperId, OrderStatus orderStatus) {
        List<Order> orderList = orderRepository.getAllByShipperIdAndStatusOrderByUpdatedAtAsc(shipperId, orderStatus);
        List<OrderReponse> orderReponseList = new ArrayList<>();
        for (Order order : orderList) {
            OrderReponse orderReponse = new OrderReponse();
            orderReponse.setOrderId(order.getId());
            orderReponse.setShipper(order.getShipper());
            orderReponse.setStatus(order.getStatus());
            orderReponse.setCustomerName(order.getUser().getLastName()+order.getUser().getFirstName());
            orderReponse.setPaymentName(order.getPayment().getName());
            orderReponse.setTotalPrice(Constant.formatter.format(order.getTotalPrice()));
            orderReponse.setShippingAddress(order.getAddress());
            orderReponseList.add(orderReponse);
        }
        return orderReponseList;
    }

    @Override
    public List<OrderReponse> findAllByShipperId(Long shipperId) {
        List<Order> orderList = orderRepository.getAllByShipperIdOrderByUpdatedAtAsc(shipperId);
        List<OrderReponse> orderReponseList = new ArrayList<>();
        for (Order order : orderList) {
            OrderReponse orderReponse = new OrderReponse();
            orderReponse.setOrderId(order.getId());
            orderReponse.setShipper(order.getShipper());
            orderReponse.setStatus(order.getStatus());
            orderReponse.setCustomerName(order.getUser().getLastName()+" "+order.getUser().getFirstName());
            orderReponse.setPaymentName(order.getPayment().getName());
            orderReponse.setTotalPrice(Constant.formatter.format(order.getTotalPrice()));
            orderReponse.setShippingAddress(order.getAddress());
            orderReponseList.add(orderReponse);
        }
        return orderReponseList;
    }

    @Override
    public OrderReponse findByOrderId(Long id){
        Order order = orderRepository.findById(id).get();
        OrderReponse orderReponse = new OrderReponse();
        orderReponse.setOrderId(order.getId());
        orderReponse.setShipper(order.getShipper());
        orderReponse.setStatus(order.getStatus());
        orderReponse.setCustomerName(order.getUser().getLastName()+" "+order.getUser().getFirstName());
        orderReponse.setPaymentName(order.getPayment().getName());
        orderReponse.setTotalPrice(Constant.formatter.format(order.getTotalPrice()));
        orderReponse.setShippingAddress(order.getAddress());
        return orderReponse;
    }

    @Override
    public List<Order> ordersByYearAndMonthForShipper(int year, int month, Long shipperId) {
        return orderRepository.ordersByYearAndMonthForShipper(year, month, shipperId);
    }

    @Override
    public List<Order> totalPriceByYearAndMonthForShipper(int year, int month, Long shipperId) {
        return orderRepository.totalPriceByYearAndMonthForShipper(year, month, shipperId);
    }

    public String totalPriceByYearAndMonthByShipper(int year, int month, Long shipperId) {
        List<Order> orderList = this.totalPriceByYearAndMonthForShipper(year, month, shipperId);
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Order order : orderList) {
            totalPrice = totalPrice.add(order.getTotalPrice());
        }
        return Constant.formatter.format(totalPrice);
    }

    @Override
    public BigDecimal getTotalPurchaseDueForDeliveredOrders() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        return deliveredOrders.stream()
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getTotalProductsSold() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);

        return deliveredOrders.stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .mapToInt(OrderDetail::getQuantity)
                .sum();
    }

    @Override
    public int getTotalOrder() {
        List<Order> orders = orderRepository.findAll();
        return orders.size();
    }

    @Override
    public int getTotalOrderForShipping() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        return deliveredOrders.size();
    }

    @Override
    public int getTotalOrderDelivered() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED);
        return deliveredOrders.size();
    }

    @Override
    public int getTotalOrderCancelled() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.CANCELLED);
        return deliveredOrders.size();
    }

    @Override
    public int getTotalOrderShipping() {
        List<Order> deliveredOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        return deliveredOrders.size();
    }

    @Override
    public List<Order> getRecentlyOrders() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(6)
                .toList();
    }

    @Override
    public void deleteFailOrder(){
        try {
            Order order = orderRepository.findTopByOrderByCreatedAtDesc().orElse(null);
            if (order != null){
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrderId(order.getId());
                for (OrderDetail orderDetail : orderDetails){
                    productService.increaseStockQuantity(orderDetail.getProduct().getId(),orderDetail.getQuantity());
                }
                orderRepository.deleteById(order.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Order> findOrderByIdWithDetails(Long orderId) {
        String jpql = "SELECT o FROM orders o " +
                "LEFT JOIN FETCH o.user " +
                "LEFT JOIN FETCH o.payment " +
                "LEFT JOIN FETCH o.address " +
                "LEFT JOIN FETCH o.voucher " +
                "LEFT JOIN FETCH o.orderDetails od " +
                "LEFT JOIN FETCH od.product " +
                "WHERE o.id = :orderId";

        try {
            Order order = entityManager.createQuery(jpql, Order.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();
            return Optional.of(order);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Order createOrderWithVoucher(User user, BigDecimal totalPrice, Voucher voucher,
                                        Payment payment, Address address, Long cartId,
                                        List<CartDetailResponse> cartDetailList) {

        // Validate dữ liệu
        validateOrderData(user, totalPrice, address, cartDetailList);

        Order order = new Order();
        order.setTotalPrice(totalPrice);
        order.setActive(true);
        order.setUser(user);
        order.setVoucher(voucher);

        // Giảm số lượng voucher
        voucherService.decreaseQuantity(voucher.getId());

        order.setPayment(payment);
        order.setAddress(address);

        // Tạo order details
        List<OrderDetail> orderDetails = createOrderDetails(order, cartId, cartDetailList);
        order.setOrderDetails(orderDetails);

        return orderRepository.save(order);
    }



    private List<OrderDetail> createOrderDetails(Order order, Long cartId,
                                                 List<CartDetailResponse> cartDetailList) {
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartDetailResponse cartDetailResponse : cartDetailList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);

            Product product = productService.findByName(cartDetailResponse.getProductName());
            orderDetail.setProduct(product);
            orderDetail.setQuantity(cartDetailResponse.getQuantity());

            // Lưu giá 1 sản phẩm tại thời điểm đặt hàng
            orderDetail.setPrice(product.getPrice());

            // Tính tổng tiền = price * quantity
            BigDecimal total = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartDetailResponse.getQuantity()));
            orderDetail.setTotalPrice(total);

            orderDetails.add(orderDetail);

            // Giảm số lượng tồn kho sản phẩm
            productService.decreaseStockQuantity(cartDetailResponse.getProductId(),
                    cartDetailResponse.getQuantity());

            // Xóa cart detail
            CartDetail cartDetail = cartDetailService
                    .findByCart_IdAndProductId(cartId, cartDetailResponse.getProductId());
            if (cartDetail != null) {
                cartDetailService.delete(cartDetail);
            }
        }

        return orderDetails;
    }

    private void validateOrderData(User user, BigDecimal totalPrice,
                                   Address address, List<CartDetailResponse> cartDetailList) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total price must be greater than 0");
        }
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        if (cartDetailList == null || cartDetailList.isEmpty()) {
            throw new IllegalArgumentException("Cart items cannot be empty");
        }
    }

    @Override
    public Map<String, Object> getSalesRevenueDataLast7Days() {
        List<Object[]> results = orderRepository.getSalesAndRevenueLast7Days();

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Tạo danh sách 7 ngày gần nhất
        List<String> dates = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> today.minusDays(6 - i).format(fmt))
                .collect(Collectors.toList());

        List<Integer> salesCount = new ArrayList<>(Collections.nCopies(7, 0));
        List<Double> revenue = new ArrayList<>(Collections.nCopies(7, 0.0)); // Đổi sang Double

        // Gán dữ liệu thực tế từ DB
        for (Object[] row : results) {
            String dbDate = row[0].toString();
            int idx = dates.indexOf(dbDate);
            if (idx != -1) {
                salesCount.set(idx, ((Number) row[1]).intValue());
                // Convert BigDecimal to Double
                BigDecimal revValue = (BigDecimal) row[2];
                revenue.set(idx, revValue != null ? revValue.doubleValue() : 0.0);
            }
        }

        return Map.of(
                "dates", dates,
                "salesCount", salesCount,
                "revenue", revenue,
                "period", "Last 7 Days"
        );
    }

    @Override
    public Map<String, Object> getOrderStatusStatistics() {
        List<Object[]> results = orderRepository.getOrderStatusCounts();

        List<String> statuses = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        List<String> colors = Arrays.asList("#2eca6a", "#ff771d", "#4154f1", "#ff0000", "#ffc107");
        int totalOrders = 0;

        for (Object[] row : results) {
            String status = row[0].toString();
            int count = ((Number) row[1]).intValue();
            statuses.add(status);
            counts.add(count);
            totalOrders += count;
        }

        return Map.of(
                "statuses", statuses,
                "counts", counts,
                "colors", colors,
                "totalOrders", totalOrders
        );
    }

    @Override
    public Map<String, Object> getMonthlyRevenueData() {
        List<Object[]> results = orderRepository.getMonthlyRevenueThisYear();

        Double[] monthlyRevenue = new Double[12]; // Đổi sang Double
        Arrays.fill(monthlyRevenue, 0.0);

        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            BigDecimal rev = (BigDecimal) row[1];
            if (month >= 1 && month <= 12) {
                monthlyRevenue[month - 1] = rev != null ? rev.doubleValue() : 0.0;
            }
        }

        return Map.of(
                "months", List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
                "revenue", Arrays.asList(monthlyRevenue),
                "year", LocalDate.now().getYear()
        );
    }






}
