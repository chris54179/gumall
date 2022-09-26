package com.xyz.gumall.ware.service;

import com.xyz.gumall.ware.vo.FareVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.ware.entity.WareInfoEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 *
 *
 *
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId);
}

