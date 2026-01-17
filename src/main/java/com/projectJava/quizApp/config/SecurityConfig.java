package com.projectJava.quizApp.config;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.CustomUserDetailsService;
import com.projectJava.quizApp.utility.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // REMOVED CustomOAuth2UserService to avoid DB conflicts

    @Autowired
    private JwtAuthenticationFilter jwtFilter;
    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private JWTUtil jwtUtil;

    @Bean
    public AuthenticationManager authManager(HttpSecurity http, PasswordEncoder encoder) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(encoder)
                .and().build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/oauth2/**", "/login/oauth2/**","/api/public/**").permitAll()
                        .requestMatchers("/quiz/create**", "/question/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth -> oauth
                        // Use default service, handle DB logic in SuccessHandler
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            try {
                OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
                String email = oauthUser.getAttribute("email");
                String name = oauthUser.getAttribute("name");
                String providerId = oauthUser.getAttribute("sub");

                // 1. Find or Create User
                Customer customer = userRepo.findByEmail(email).orElse(null);
                boolean isNewUser = false;

                if (customer == null) {
                    isNewUser = true; // Flag to trigger onboarding
                    customer = new Customer();
                    customer.setEmail(email);
                    customer.setUsername(name != null ? name : email);
                    customer.setPassword(null);

                    // CRITICAL: Set role to PENDING so we know they need to choose
                    customer.setRole("PENDING");

                    customer.setProvider("GOOGLE");
                    customer.setProviderId(providerId);
                    customer.setVerified(true);
                    customer = userRepo.save(customer);
                } else {
                    // Existing logic for reactivation/linking...
                    if (customer.getDeletedAt() != null) {
                        // ... handle reactivation checks here (same as before) ...
                        customer.setDeletedAt(null);
                    }
                    // Ensure provider is linked
                    if (customer.getProvider() == null) {
                        customer.setProvider("GOOGLE");
                        customer.setProviderId(providerId);
                        customer.setVerified(true);
                        userRepo.save(customer);
                    }
                }

                // 2. Generate Token
                // If role is PENDING, we still give them a token so they can call the "Update Role" API
                UserDetails userDetails = new User(
                        customer.getUsername(),
                        "",
                        Collections.singleton(new SimpleGrantedAuthority(customer.getRole()))
                );
                String token = jwtUtil.generateToken(userDetails);

                // 3. Set Cookie
                ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(Duration.ofMinutes(30))
                        .sameSite("None")
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                // 4. SMART REDIRECT
                // If their role is still PENDING, send them to the selection page
                if ("PENDING".equals(customer.getRole())) {
                    response.sendRedirect("https://localhost:4300/select-role");
                } else {
                    // Normal login
                    if("STUDENT".equals(customer.getRole())){
                        response.sendRedirect("https://localhost:4300/student-dashboard");

                    }
                    else if("INSTRUCTOR".equals(customer.getRole())){
                        response.sendRedirect("https://localhost:4300/dashboard");
                    }
                    else{
                        response.sendRedirect("https://localhost:4300/user-dashboard");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect("https://localhost:4300/login?error=processing_failed");
            }
        };
    }
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("https://localhost:4300")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}