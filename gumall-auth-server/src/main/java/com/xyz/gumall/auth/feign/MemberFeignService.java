package com.xyz.gumall.auth.feign;

import com.xyz.common.utils.R;
import com.xyz.gumall.auth.vo.SocialUser;
import com.xyz.gumall.auth.vo.UserLoginVo;
import com.xyz.gumall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "gumall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo registerVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo loginVo);

    @PostMapping("member/member/oauth2/login")
    R oauthlogin(@RequestBody SocialUser socialUser);
}
