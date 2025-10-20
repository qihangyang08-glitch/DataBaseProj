package com.example.taskplanning.aop;

import com.example.taskplanning.annotation.LogAction;
import com.example.taskplanning.entity.ActionLog;
import com.example.taskplanning.entity.User;
import com.example.taskplanning.service.LogService;
import com.example.taskplanning.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    private final LogService logService;
    private final UserService userService;

    @Autowired
    public LoggingAspect(LogService logService, UserService userService) {
        this.logService = logService;
        this.userService = userService;
    }

    @AfterReturning(pointcut = "@annotation(com.example.taskplanning.annotation.LogAction)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogAction logAction = method.getAnnotation(LogAction.class);
            if (logAction == null) return;

            String action = logAction.action();
            String entityType = logAction.entityType();

            Long entityId = extractEntityId(joinPoint.getArgs(), result);

            String ip = null;
            String userAgent = null;
            RequestAttributes reqAttr = RequestContextHolder.getRequestAttributes();
            if (reqAttr instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) reqAttr).getRequest();
                if (request != null) {
                    ip = request.getRemoteAddr();
                    userAgent = request.getHeader("User-Agent");
                }
            }

            User currentUser = null;
            try {
                currentUser = userService.getCurrentUserEntity();
            } catch (Exception ignored) {}

            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("method", method.getName());
            detailsMap.put("args", buildArgMap(signature.getParameterNames(), joinPoint.getArgs()));
            detailsMap.put("result", result);

            String details = detailsMap.toString();

            ActionLog actionLog = new ActionLog(currentUser, action, entityType, entityId, details, ip, userAgent);
            logService.log(actionLog);
        } catch (Throwable t) {
            logger.error("LoggingAspect error", t);
        }
    }

    private Map<String, Object> buildArgMap(String[] names, Object[] values) {
        Map<String, Object> map = new HashMap<>();
        if (names == null) return map;
        for (int i = 0; i < names.length && i < values.length; i++) {
            map.put(names[i], values[i]);
        }
        return map;
    }

    private Long extractEntityId(Object[] args, Object result) {
        for (Object arg : args) {
            if (arg instanceof Long) return (Long) arg;
            try {
                Method m = arg != null ? arg.getClass().getMethod("getId") : null;
                if (m != null) {
                    Object id = m.invoke(arg);
                    if (id instanceof Long) return (Long) id;
                }
            } catch (Exception ignored) {}
        }

        if (result != null) {
            try {
                Method m = result.getClass().getMethod("getId");
                Object id = m.invoke(result);
                if (id instanceof Long) return (Long) id;
            } catch (Exception ignored) {}
        }

        return null;
    }

}
