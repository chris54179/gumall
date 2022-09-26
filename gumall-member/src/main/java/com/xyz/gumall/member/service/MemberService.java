package com.xyz.gumall.member.service;

import com.xyz.gumall.member.exception.PhoneExistException;
import com.xyz.gumall.member.exception.UsernameExistException;
import com.xyz.gumall.member.vo.MemberLoginVo;
import com.xyz.gumall.member.vo.MemberRegisterVo;
import com.xyz.gumall.member.vo.SocialUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xyz.common.utils.PageUtils;
import com.xyz.gumall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 *
 *
 *
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegisterVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUsernameUnique(String username) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser);
}

