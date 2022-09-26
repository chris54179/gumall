package com.xyz.gumall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

public class Cart {
    /**
     * 购物车子项信息
     */
    List<CartItem> items;

    /**
     * 商品数量
     */
    private Integer countNum;

    /**
     * 商品类型数量
     */
    private Integer countType;

    /**
     * 商品总价
     */
    private BigDecimal totalAmount;

    /**
     * 减免价格
     */
    private BigDecimal reduce = new BigDecimal("0.00");

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer getCountNum() {
        int count=0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

//    public void setCountNum(Integer countNum) {
//        this.countNum = countNum;
//    }

    public Integer getCountType() {
        int count=0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += 1;
            }
        }
        return count;
    }

//    public void setCountType(Integer countType) {
//        this.countType = countType;
//    }

    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal("0");
        // 1、计算购物项总价
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                if (item.getCheck()) {
                    // 拿到购物车中单个商品的总金额
                    BigDecimal totalPrice = item.getTotalPrice();
                    // 添加到购物总价中
                    amount = amount.add(totalPrice);
                }
            }
        }
        // 2、减去优惠总价
        BigDecimal subtract = amount.subtract(getReduce());

        return subtract;
    }

//    public void setTotalAmount(BigDecimal totalAmount) {
//        this.totalAmount = totalAmount;
//    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }
}
