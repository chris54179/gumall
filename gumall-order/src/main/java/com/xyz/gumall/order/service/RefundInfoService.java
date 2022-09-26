package com.xyz.gumall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.order.entity.RefundInfoEntity;

import java.util.Map;

/**
 * 退款信息
 *
 *
 *
 *
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

