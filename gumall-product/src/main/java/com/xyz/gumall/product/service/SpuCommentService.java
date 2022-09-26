package com.xyz.gumall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.product.entity.SpuCommentEntity;

import java.util.Map;

/**
 * 商品评价
 *
 *
 *
 *
 */
public interface SpuCommentService extends IService<SpuCommentEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

