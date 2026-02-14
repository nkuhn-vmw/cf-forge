package com.cfforge.agent.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class McpAuthFilter implements Filter {

    @Value("${cfforge.mcp.api-key:changeme}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();
        if (path.startsWith("/mcp")) {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.equals("Bearer " + apiKey)) {
                ((HttpServletResponse) res).setStatus(401);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
