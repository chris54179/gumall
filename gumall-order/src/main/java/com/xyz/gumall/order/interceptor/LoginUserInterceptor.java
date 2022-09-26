package com.xyz.gumall.order.interceptor;

import com.xyz.common.constant.AuthServerConstant;
import com.xyz.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = new AntPathMatcher().match("/order/order/status/**", uri);
        boolean match1 = new AntPathMatcher().match("/payed/notify", uri);
        if (match || match1) {
            return true;
        }

        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute != null) {
            loginUser.set(attribute);
            return true;
        }else {
            request.getSession().setAttribute("msg", "請先進行登入");
            response.sendRedirect("http://auth.gumall.com/login.html");
            return false;
        }
    }
}
