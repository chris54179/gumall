package com.xyz.gumall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    // 收货地址，ums_member_receive_address
    @Getter @Setter
    List<MemberAddressVo> address;

    // 所有选中的购物项
    @Getter @Setter
    List<OrderItemVo> items;

    // 发票记录...

    /**
     * 优惠卷信息
     */
    @Getter @Setter
    Integer integration;

    @Getter @Setter
    Map<Long, Boolean> stocks;

    //防重令牌
    @Getter @Setter
    private String orderToken;

    public Integer getCount(){
        Integer i = 0;
        if(items != null) {
            for (OrderItemVo item : items) {
                i+= item.getCount();
            }
        }
        return i;
    }

    /**
     * 订单总额
     */
//    BigDecimal total;
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items != null) {
            for (OrderItemVo orderItemVo : items) {
                BigDecimal multiply = orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }
    /**
     * 应付价格
     */
//    BigDecimal payPrice;
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
