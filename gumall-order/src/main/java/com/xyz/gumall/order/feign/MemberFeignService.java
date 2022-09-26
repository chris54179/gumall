package com.xyz.gumall.order.feign;

import com.xyz.gumall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gumall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    public List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);
}
