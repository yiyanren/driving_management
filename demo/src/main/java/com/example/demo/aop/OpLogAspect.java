package com.example.demo.aop;

import com.example.demo.service.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OpLogAspect {
    private final OperationLogService operationLogService;

    public OpLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        String operator = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            operator = auth.getName();
        }
        try {
            Object result = pjp.proceed();
            operationLogService.save(opLog.module(), opLog.action(), operator, "SUCCESS", pjp.getSignature().toShortString());
            return result;
        } catch (Throwable ex) {
            operationLogService.save(opLog.module(), opLog.action(), operator, "FAIL", ex.getMessage());
            throw ex;
        }
    }
}
