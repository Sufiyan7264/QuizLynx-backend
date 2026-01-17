package com.projectJava.quizApp.config;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.CustomUserDetailsService;
import com.projectJava.quizApp.utility.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepo userRepo;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("JWT Filter checking path: {} with method: {}", path, method);

        // Skip JWT filter for OAuth2-related endpoints
        boolean shouldSkip = path.startsWith("/login/oauth2") ||
                path.startsWith("/oauth2") ||
                path.equals("/error") ||
                path.equals("/login") ;
        if (shouldSkip) {
            log.debug("Skipping JWT filter for path: {}", path);
        }

        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("JWT Filter processing path: {}", path);

        String token = null;
        String username = null;

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                log.debug("Found Bearer token in Authorization header");
            } else {
                if (request.getCookies() != null) {
                    for (var cookie : request.getCookies()) {
                        if ("JWT_TOKEN".equals(cookie.getName())) {
                            token = cookie.getValue();
                            log.debug("Found JWT token in cookie");
                            break;
                        }
                    }
                }
            }

            if (token != null) {
                username = jwtUtil.extractUsername(token);
                log.debug("Extracted username from token: {}", username);
            }
        } catch (Exception ex) {
            log.warn("Failed to parse/validate JWT: {}", ex.getMessage());
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token, userDetails)) {
                    try {
                        Date tokenIssuedAt = jwtUtil.extractIssuedAt(token);
                        Instant tokenIat = (tokenIssuedAt == null) ? null : tokenIssuedAt.toInstant();

                        Optional<Customer> customerOpt = userRepo.findByUsername(username);
                        if(customerOpt.isPresent()){
                            Customer customer = customerOpt.get();
                            Instant passwordChangedAt = customer.getPasswordChangedAt();
                            if(passwordChangedAt != null && tokenIat !=null && tokenIat.isBefore(passwordChangedAt)){
                                log.info("Rejecting JWT for user {}: token iat {} is before passwordChangedAt {}",
                                        username, tokenIat, passwordChangedAt);
                            }
                            else{
                                String role = jwtUtil.extractRole(token);
                                var authorities = Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + role)
                                );

                                UsernamePasswordAuthenticationToken authToken =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                                log.debug("Successfully authenticated user: {}", username);

                            }
                        }else{
                            log.warn("User {} not found while checking passwordChangedAt", username);
                        }
                    }catch (Exception e){
                        log.warn("Error while validating token issued-at vs passwordChangedAt: {}", e.getMessage());
                    }

                } else {
                    log.debug("JWT validation failed for user: {}", username);
                }
            } catch (Exception ex) {
                log.warn("Could not set user authentication: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
