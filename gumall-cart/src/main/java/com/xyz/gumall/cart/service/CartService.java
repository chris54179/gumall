package com.xyz.gumall.cart.service;

import com.xyz.gumall.cart.vo.Cart;
import com.xyz.gumall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    void clearCart(String cartkey);

    void checkItem(Long skuId, Integer check);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getUserCartItems();

//    CartItem getCartItem(Long skuId);
//
//    Cart getCart();
//
//    void checkCart(Long skuId, Integer isChecked);
//
//    void changeItemCount(Long skuId, Integer num);
//
//    void deleteItem(Long skuId);
//
//    List<CartItem> getCheckedItems();
}
