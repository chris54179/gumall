package com.xyz.gumall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.member.entity.MemberReceiveAddressEntity;

import java.util.List;
import java.util.Map;

/**
 * 会员收货地址
 *
 *
 *
 *
 */
public interface MemberReceiveAddressService extends IService<MemberReceiveAddressEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<MemberReceiveAddressEntity> getAddress(Long memberId);
}

