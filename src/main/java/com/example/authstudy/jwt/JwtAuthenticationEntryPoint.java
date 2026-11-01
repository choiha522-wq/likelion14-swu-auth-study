package com.example.authstudy.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        Object error = request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        String code = "EXPIRED_TOKEN".equals(error) ? "EXPIRED_TOKEN"
                : "INVALID_TOKEN".equals(error) ? "INVALID_TOKEN" : "UNAUTHORIZED";
        String message = "EXPIRED_TOKEN".equals(error) ? "토큰이 만료되었습니다."
                : "INVALID_TOKEN".equals(error) ? "토큰이 유효하지 않습니다." : "인증이 필요합니다.";
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
