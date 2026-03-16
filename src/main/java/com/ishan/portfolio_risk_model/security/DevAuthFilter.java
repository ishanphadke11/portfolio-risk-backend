package com.ishan.portfolio_risk_model.security;

// TODO: REMOVE — dev-only auth bypass. Delete this file and revert SecurityConfig when auth is fixed.

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * DEV ONLY — auto-authenticates every request as the test user so the frontend
 * can call protected endpoints without a valid JWT token.
 *
 * Remove this filter (and revert SecurityConfig) once auth is working correctly.
 */
// TODO: REMOVE — delete @Profile restriction once auth is fully disabled in all envs
@Component
public class DevAuthFilter extends OncePerRequestFilter {

    static final String DEV_USER_EMAIL = "test@factorlens.com";

    private final UserDetailsService userDetailsService;

    public DevAuthFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only inject when no auth is already set (JWT filter runs first and may have set one)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(DEV_USER_EMAIL);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // test user not in DB yet — continue unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }
}
