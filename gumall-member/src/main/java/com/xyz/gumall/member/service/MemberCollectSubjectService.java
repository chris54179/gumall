package com.xyz.gumall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.member.entity.MemberCollectSubjectEntity;

import java.util.Map;

/**
 * 会员收藏的专题活动
 *
 *
 *
 *
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

