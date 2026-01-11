package com.clouddisk.interceptor;

import com.clouddisk.common.BusinessException;
import com.clouddisk.entity.User;
import com.clouddisk.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 管理员权限拦截器
 * 确保只有管理员（userType=2）才能访问管理员接口
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录");
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查是否为管理员
        if (user.getUserType() != 2) {
            log.warn("非管理员尝试访问管理员接口: userId={}, userType={}, path={}",
                    userId, user.getUserType(), request.getRequestURI());
            throw new BusinessException("无权访问管理员功能");
        }

        return true;
    }
}
