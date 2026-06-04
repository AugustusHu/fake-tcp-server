package com.example.faketcp.api;

import com.example.faketcp.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ApiSecurity {
    private ApiSecurity() {
    }

    static boolean isAdmin(UserDto user) {
        return user != null && (user.isAdmin() || "admin".equalsIgnoreCase(user.getUsername()));
    }

    static void requireAdmin(UserDto user) {
        if (!isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有 admin 可以访问用户管理");
        }
    }

    static void requireSelfOrAdmin(UserDto user, long userId) {
        if (user == null || user.getId() == null || (!user.getId().equals(userId) && !isAdmin(user))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能访问当前用户自己的数据");
        }
    }

    static void requireSelf(UserDto user, long userId) {
        if (user == null || user.getId() == null || !user.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能访问当前用户自己的数据");
        }
    }
}
