package com.geetrk.ecommerce.service;

import com.geetrk.ecommerce.model.CartItem;
import com.geetrk.ecommerce.model.Order;
import com.geetrk.ecommerce.model.OrderItem;
import com.geetrk.ecommerce.model.OrderItemDTO;
import com.geetrk.ecommerce.model.OrderResponse;
import com.geetrk.ecommerce.model.OrderStatus;
import com.geetrk.ecommerce.model.User;
import com.geetrk.ecommerce.repository.OrderRepository;
import com.geetrk.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public Optional<OrderResponse> createOrder(String userId) {

        List<CartItem> cartItems = cartService.getCart(userId);
        if(cartItems.isEmpty()) {
            return Optional.empty(); // No items in cart
        }
        Optional<User> userOptional = userRepository.findById(Long.valueOf(userId));
        if (userOptional.isEmpty()) {
            return Optional.empty(); // User not found
        }

        User user = userOptional.get();

        BigDecimal totalAmount = cartItems.stream()
                .map(CartItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(totalAmount);

        List<OrderItem> orderItems = cartItems.stream()
                .map(item -> new OrderItem(
                        null,
                        item.getProduct(),
                        item.getQuantity(),
                        item.getPrice(),
                        order
                ))
                .toList();

        order.setItems(orderItems);
        Order orderSaved = orderRepository.save(order);

        cartService.clearCart(userId);

        return Optional.of(mapToOrderResponse(orderSaved));
    }


    public OrderResponse mapToOrderResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getItems().stream()
                        .map( orderItem -> new OrderItemDTO(
                                orderItem.getId(),
                                orderItem.getProduct().getId(),
                                orderItem.getQuantity(),
                                orderItem.getPrice(),
                                orderItem.getPrice().multiply(new BigDecimal(orderItem.getQuantity()))
                                ))
                        .toList(),
                order.getCreatedAt()
        );
    }
}
