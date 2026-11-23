package com.hsis.registrarapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {

        // 1. Look for the "Authorization" header in the incoming request
        String authHeader = request.getHeader("Authorization");

        // 2. If it's missing, or doesn't start with "Bearer ", ignore it and let the request continue to be blocked
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Cut off the first 7 characters ("Bearer ") to isolate just the JWT string
        String token = authHeader.substring(7);

       
        // 4. Ask JwtUtil to verify the cryptographic lock
        if (jwtUtil.validateToken(token)) {
            // 5. If valid, extract the email AND the roles
            String email = jwtUtil.extractEmail(token);
            List<String> roles = jwtUtil.extractRoles(token);

            // 6. Convert the text roles into official Spring Security badges
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .collect(Collectors.toList());

            // 7. Write the user's identity AND their new badges onto the clipboard
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(email, null, authorities); // <-- NO MORE EMPTY LIST!
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        

        // 7. Allow the request to move forward to the Controller
        filterChain.doFilter(request, response);
    }
}