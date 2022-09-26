package com.xyz.gumall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.order.entity.OrderItemEntity;

import java.util.Map;

/**
 * 订单项信息
 *
 *
 *
 *
 */
public interface OrderItemService extends IService<OrderItemEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

