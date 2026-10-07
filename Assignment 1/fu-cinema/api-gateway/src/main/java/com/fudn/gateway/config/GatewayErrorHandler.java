package com.fudn.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/** Security failures have the same JSON contract as downstream business errors. */
public class GatewayErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "A valid Bearer token is required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Your role cannot access this endpoint");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(Map.of(
                "timestamp", LocalDateTime.now().toString(), "status", status.value(),
                "error", status.getReasonPhrase(), "message", message, "path", request.getRequestURI())));
    }
}
