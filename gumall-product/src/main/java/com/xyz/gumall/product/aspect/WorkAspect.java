package com.xyz.gumall.product.aspect;

//import io.seata.core.context.RootContext;
//import io.seata.core.exception.TransactionException;
//import io.seata.tm.api.GlobalTransaction;
//import io.seata.tm.api.GlobalTransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


@Slf4j
@Aspect
@Component
public class WorkAspect {

    @Before(value = "execution(* com.xyz.gumall.product.service.impl.*.*(..))")
    public void before(JoinPoint joinPoint) throws Exception {
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        log.info("拦截方法," + method.getName());
    }

    @AfterThrowing(throwing = "e", pointcut = "execution(* com.xyz.gumall.product.service.impl.*.*(..))")
    public void doRecoveryActions(Throwable e) throws Exception {
        log.info("方法执行异常:{}", e.getMessage());
    }

}
