package com.geetrk.ecommerce.service;

import com.geetrk.ecommerce.dto.CartItemRequest;
import com.geetrk.ecommerce.model.CartItem;
import com.geetrk.ecommerce.model.Product;
import com.geetrk.ecommerce.model.User;
import com.geetrk.ecommerce.repository.CartItemRepository;
import com.geetrk.ecommerce.repository.ProductRepository;
import com.geetrk.ecommerce.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public boolean addtoCart(String userId, CartItemRequest cartItemRequest) {
        //Look for product

    Optional<Product> productOpt = productRepository.findById(cartItemRequest.getProductId());
    if (productOpt.isEmpty()) {
        return false; // Product not found
    }

    Product product = productOpt.get();
    if(product.getStockQuantity() < cartItemRequest.getQuantity()) {
        return false; // Not enough stock
    }

    Optional<User> userOpt = userRepository.findById(Long.valueOf(userId));
    if(userOpt.isEmpty()) {
        return false; // User not found
    }
    User user = userOpt.get();

    CartItem existingCartItem = cartItemRepository.findByUserAndProduct(user, product);
    if(existingCartItem != null) {
        existingCartItem.setQuantity(existingCartItem.getQuantity() + cartItemRequest.getQuantity());
        existingCartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(existingCartItem.getQuantity())));
        cartItemRepository.save(existingCartItem);
    } else {
        CartItem newCartItem = new CartItem();
        newCartItem.setUser(user);
        newCartItem.setProduct(product);
        newCartItem.setQuantity(cartItemRequest.getQuantity());
        newCartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));
        cartItemRepository.save(newCartItem);
    }
    return true;
    }

    public boolean deleteItemFromCart(String userId, String productId) {

        Optional<Product> productOpt = productRepository.findById(Long.valueOf(productId));
        Optional<User> userOpt = userRepository.findById(Long.valueOf(userId));

        if (productOpt.isPresent() && userOpt.isPresent()){
            cartItemRepository.deleteByUserAndProduct(userOpt.get(), productOpt.get());
            return true;
        }
        return false;
    }

    public List<CartItem> getCart(String userId) {
        return userRepository.findById(Long.valueOf(userId))
                .map(cartItemRepository::findByUser)
                .orElseGet(List::of);
    }

    public void clearCart(String userId) {

        userRepository.findById(Long.valueOf(userId)).ifPresent(
                cartItemRepository::deleteByUser);
    }
}
