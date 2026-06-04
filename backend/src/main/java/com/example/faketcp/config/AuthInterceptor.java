package com.example.faketcp.config;

import com.example.faketcp.dto.UserDto;
import com.example.faketcp.service.AuthService;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Optional<UserDto> currentUser = authService.resolve(request.getHeader("Authorization"));
        if (!currentUser.isPresent()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"未登录或登录已过期\"}");
            return false;
        }
        request.setAttribute("currentUser", currentUser.get());
        return true;
    }
}
