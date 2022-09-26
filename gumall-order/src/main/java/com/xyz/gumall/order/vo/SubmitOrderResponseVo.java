package com.xyz.gumall.order.vo;

import com.xyz.gumall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

    /** 错误状态码 0成功 **/
    private Integer code;
}
