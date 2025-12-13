package com.project.familytree.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.dto.CustomApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityResponseUtil {
    private final ObjectMapper objectMapper;

    public void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        CustomApiResponse<Void> errorResponse = CustomApiResponse.error(message, null);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
