package com.xyz.gumall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

/**
 * δΈι’εε
 *
 *
 *
 *
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

