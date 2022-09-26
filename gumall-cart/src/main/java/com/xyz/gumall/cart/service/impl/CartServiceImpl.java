package com.xyz.gumall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xyz.common.utils.R;
import com.xyz.gumall.cart.feign.ProductFeignService;
import com.xyz.gumall.cart.interceptor.CartInterceptor;
import com.xyz.gumall.cart.vo.Cart;
import com.xyz.gumall.cart.vo.SkuInfoVo;
import com.xyz.gumall.cart.vo.UserInfoTo;
import com.xyz.gumall.cart.service.CartService;
import com.xyz.gumall.cart.vo.CartItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ThreadPoolExecutor executor;


    private final String CART_PREFIX = "gumall:cart:";

    /**
     * 给购物车里面添加商品
     * @param skuId 商品id
     * @param num 数量
//     * @throws ExecutionException
     * @throws InterruptedException
     * 如果远程查询比较慢，比如方法当中有好几个远程查询，都要好几秒以上，等整个方法返回就需要很久，这块你是怎么处理的?
     *  1、为了提交远程查询的效率，可以使用线程池的方式，异步进行请求
     *  2、要做的操作就是将所有的线程全部放到自己手写的线程池里面
     *  3、每一个服务都需要配置一个自己的线程池
     *  4、完全使用线程池来控制所有的请求
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        // 获取到要操作的购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 1、如果购物车中已有该商品那么需要做的就是修改商品的数量，如果没有该商品需要进行添加该商品到购物车中
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)){
            // 2、添加商品到购物车
            // 第一个异步任务,查询当前商品信息
            CartItem cartItem = new CartItem(); // 购物车中每一个都是一个购物项，封装购物车的内容
            CompletableFuture<Void> completableFutureSkuInfo = CompletableFuture.runAsync(() -> {
                // 2.1、远程查询出当前要添加的商品信息
                // 添加哪个商品到购物车，先查询到这个商品的信息
                R skuInfo = productFeignService.getSkuInfo(skuId);
                // 获取远程返回的数据，远程数据都封装在skuInfo中
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setCheck(true); // 添加商品到购物车，那么这个商品一定是选中的
                cartItem.setSkuId(skuId); // 查询的是哪个商品的id，那么这个商品id就是哪个
                cartItem.setImage(data.getSkuDefaultImg()); // 当前商品的图片信息
                cartItem.setPrice(data.getPrice()); // 当前商品的价格
                cartItem.setTitle(data.getSkuTitle()); // 当前商品的标题
                cartItem.setCount(num); // 当前商品添加的数量
            }, executor);
            // 3、第二个异步任务，远程查询sku组合信息
            CompletableFuture<Void> completableFutureSkuAttr = CompletableFuture.runAsync(() -> {
                List<String> values = productFeignService.getSkuSaleAttrValues(skuId); // 根据skuid来查
                cartItem.setSkuAttr(values); // 远程查询出来到sku组合信息，需要在购物车中进行显示
            }, executor);
            // 4、两个异步任务都完成，才能把数据放到redis中
            CompletableFuture.allOf(completableFutureSkuInfo,completableFutureSkuAttr).get();
            // 把购物项的数据保存redis中
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(),s); // 添加商品到购物车中
            return cartItem;
        }
        else {
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount()+num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
    /**
     * 需求分析1
     *   1、如果用户登录后，那么临时购物车的数据如何处理？
     *      将临时购物车的数据，放到用户购物车中进行合并
     *   2、如何显示购物车中的数据？
     *      从redis中查询到数据放到对象中，返回到页面进行渲染
     */
        Cart cart = new Cart();
        // 1、购物车的获取操作，分为登录后购物车 和 没登录购物车
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get(); // 拿到共享数据
        if (userInfoTo.getUserId() != null) { // 登录后的购物车
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 如果临时购物车中还有数据，那就需要将临时购物车合并到已登录购物车里面
            // 判断临时购物车中是否有数据
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null ) { // 临时购物车中有数据，需要将临时购物车的数据合并到登录后的购物车
                for (CartItem item : tempCartItems) { // 拿到临时购物车的所有数据，将他添加到已登录购物车里面来
                    // 调用addToCart()这个方法，他会根据登录状态进行添加，当前是登录状态
                    // 所以这个方法会将临时购物车的数据添加到已登录购物车中
                    addToCart(item.getSkuId(),item.getCount()); // 合并临时和登录后的购物车
                }
                // 合并完成后还需要将临时购物车中的数据删除
                clearCart(tempCartKey);
            }
            // 再次获取用户登录后的购物车【包含临时购物车数据和已登录购物车数据】
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);// 将多个购物项设置到购物车中
        } else {
            // 没有登录的购物车，拿到没有登录购物车的数据
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            // 获取购物车中的所有购物项
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";

        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX+userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX+ userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }

    /**
     * 获取指定用户 (登录用户/临时用户) 购物车里面的数据
     * @param cartKey
     * @return
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values(); // 拿到多个购物项
        if (values != null && values.size()>0) {
            List<CartItem> collect = values.stream().map((obj) -> {
                String s = (String) obj;
                CartItem cartItem = JSON.parseObject(s, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public void clearCart(String cartkey) {
        redisTemplate.delete(cartkey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());

    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        }else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            List<CartItem> collect = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item->{
                        R price = productFeignService.getPrice(item.getSkuId());
                        // TODO: 更新為最新價格
                        String data = (String) price.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }
    }
}
