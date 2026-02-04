package com.ishan.portfolio_risk_model.security;

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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // get the authorization header
        final String authHeader = request.getHeader("Authorization");

        // check if header exists and starts with "Bearer"
        // if not skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // extract the token (remove bearer prefix)
        final String token = authHeader.substring(7);
        // extract email
        final String userEmail = jwtService.extractEmail(token);

        // if we have an email AND user is not already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // load user details from database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(token, userDetails)) {

                // create an authentication token to tell sprin security the user is authenticated
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,  // the user
                        null,  // credentials (null because we have already verified)
                        userDetails.getAuthorities()  // user's roles/permissions
                );

                // add request details (ip, session ID, etc.)
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // set the authentication in Security context so spring know user is authenticated
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // continue to next filter
        filterChain.doFilter(request, response);
    }
}
