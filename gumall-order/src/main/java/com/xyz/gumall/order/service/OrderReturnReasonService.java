package com.xyz.gumall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.order.entity.OrderReturnReasonEntity;

import java.util.Map;

/**
 * ้่ดงๅๅ 
 *
 *
 *
 *
 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

