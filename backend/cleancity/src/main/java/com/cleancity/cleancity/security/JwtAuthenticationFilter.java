package com.cleancity.cleancity.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // Login aur registration endpoints par JWT required nahi hai.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // DEBUG: Check karo ki token request ke saath aaya hai ya nahi.
        System.out.println("========== JWT DEBUG ==========");
        System.out.println("Request: "
                + request.getMethod() + " " + request.getRequestURI());

        System.out.println("Authorization header present: "
                + (authHeader != null));

        System.out.println("Bearer token present: "
                + (authHeader != null
                && authHeader.startsWith("Bearer ")));

        // Token nahi mila toh request aage bhejo.
        // Protected endpoints ko SecurityConfig reject karega.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            System.out.println("JWT RESULT: Token missing or invalid header format");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            // JWT se email extract karo.
            String email = jwtService.extractEmail(token);

            System.out.println("JWT email extracted successfully: " + email);

            Authentication existingAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();

            // Agar user already authenticated nahi hai.
            if (existingAuthentication == null
                    || !existingAuthentication.isAuthenticated()) {

                // Database se user details load karo.
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                // Token valid hai ya nahi check karo.
                if (!jwtService.isTokenValid(token, email)) {

                    SecurityContextHolder.clearContext();

                    writeErrorResponse(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid or expired JWT token"
                    );
                    return;
                }

                // User ke roles ke saath authentication banao.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // Authentication ko current request ke context mein set karo.
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                System.out.println("Authenticated email: "
                        + userDetails.getUsername());

                System.out.println("User authorities: "
                        + userDetails.getAuthorities());

                System.out.println("JWT RESULT: Authentication successful");

            } else {

                System.out.println("Existing authentication: "
                        + existingAuthentication.getAuthorities());
            }

        } catch (JwtException | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();

            System.out.println("JWT ERROR: Token validation failed");

            writeErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired JWT token"
            );
            return;

        } catch (UsernameNotFoundException ex) {

            SecurityContextHolder.clearContext();

            System.out.println("JWT ERROR: User not found in database");

            writeErrorResponse(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "User associated with token was not found"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"error\":\"" + message + "\"}"
        );
    }
}