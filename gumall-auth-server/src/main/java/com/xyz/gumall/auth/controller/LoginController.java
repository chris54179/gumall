package com.xyz.gumall.auth.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.xyz.common.constant.AuthServerConstant;
import com.xyz.common.exception.BizCodeEnume;
import com.xyz.common.utils.R;
import com.xyz.common.vo.MemberRespVo;
import com.xyz.gumall.auth.feign.MemberFeignService;
import com.xyz.gumall.auth.feign.ThirdPartFeignService;
import com.xyz.gumall.auth.vo.UserLoginVo;
import com.xyz.gumall.auth.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){
        // TODO 1、接口防刷
        // 先从redis中拿取
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)) {
            // 拆分
            long l = Long.parseLong(redisCode.split("_")[1]);
            // 当前系统事件减去之前验证码存入的事件 小于60000毫秒=60秒
            if (System.currentTimeMillis() -l < 60000) {
                // 60秒内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 2、验证码的再次效验
        // 数据存入 =》redis key-phone value - code sms:code:131xxxxx - >45678
        String code = UUID.randomUUID().toString().substring(0, 5);
        String substring = code+"_"+System.currentTimeMillis();
        // redis缓存验证码 防止同一个phone在60秒内发出多次验证吗
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone, substring,10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone, code);
        return R.ok();
    }

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if (result.hasErrors()) {
            // 拿到错误信息转换成Map
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            //用一次的属性
            redirectAttributes.addFlashAttribute("errors",errors);
            // 校验出错，转发到注册页
            return "redirect:http://auth.gumall.com/reg.html";
        }

        // 将传递过来的验证码 与 存redis中的验证码进行比较
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)) {
            // 验证码和redis中的一致
            if(code.equals(s.split("_")[0])) {
                // 删除验证码：令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                // 调用远程服务，真正注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0) {
                    // 远程调用注册服务成功
                    return "redirect:http://auth.gumall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg",r.getData("msg", new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gumall.com/reg.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("code", "验证码错误");
                // 校验出错，转发到注册页
                return "redirect:http://auth.gumall.com/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("code", "验证码错误");
            // 校验出错，转发到注册页
            return "redirect:http://auth.gumall.com/reg.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            return "login";
        }else {
            return "redirect:http://gumall.com";
        }

    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0) {
            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://gumall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gumall.com/login.html";
        }
    }

    //spring security登入
//    @PostMapping("/login")
//    @ResponseBody
//    public Object login(@RequestBody UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session, HttpServletResponse response){
//        JSONObject userInfo = new JSONObject();
//        userInfo.put("username", "myname");
//        List<String> roles = new ArrayList<>();
//        roles.add("Admin");
//        roles.add("Dev");
//        userInfo.put("roles", roles);
//        response.addHeader("AccountInfo", userInfo.toJSONString());
//
//        JSONObject result = new JSONObject();
//        result.put("code", 0);
//
////        vo.setLoginacct("aaaaaa");
////        vo.setPassword("111111");
//        R login = memberFeignService.login(vo);
//        if (login.getCode() == 0) {
//            MemberRespVo data = login.getData("data", new TypeReference<MemberRespVo>() {
//            });
//            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
//            return result;
//
////            return "redirect:http://gumall.com";
//        } else {
//            Map<String, String> errors = new HashMap<>();
//            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
//            redirectAttributes.addFlashAttribute("errors", errors);
//            return result;
//
////            return "redirect:http://auth.gumall.com/login.html";
//        }
//    }

    @GetMapping("/logout")
    public String logout(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        session.invalidate();
        return "redirect:http://gumall.com";
    }
//    @GetMapping("/login.html")
//    public String loginPage(){
//        return "login";
//    }
//
//    @GetMapping("/reg.html")
//    public String regPage(){
//        return "reg";
//    }
}
