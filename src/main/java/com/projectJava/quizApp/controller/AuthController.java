package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.ChangePassRequest;
import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.RefreshToken;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.CustomUserDetailsService;
import com.projectJava.quizApp.service.CustomerService;
import com.projectJava.quizApp.service.OtpService;
import com.projectJava.quizApp.service.RefreshTokenService;
import com.projectJava.quizApp.utility.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.Ref;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired(required = true)
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepo userRepo;

    @Autowired private RefreshTokenService refreshTokenService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private OtpService otpService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // LOGIN: now sets HttpOnly cookie + Authorization header (for backward compatibility)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData,
                                   HttpServletResponse response) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        // 1. Authenticate (Check if password is correct)
        // Note: Even if soft-deleted, the password check will pass first.
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
        }

        // 2. Fetch the Customer Entity to check "deletedAt"
        Customer customer = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User found in Auth but not in Repo?"));

        // 3. CHECK DELETION STATUS
        if (customer.getDeletedAt() != null) {
            long daysSinceDeletion = ChronoUnit.DAYS.between(customer.getDeletedAt(), Instant.now());

            if (daysSinceDeletion > 30) {
                // Scenario: Grace period over.
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "This account has been permanently deleted. Please contact support or register again."));
            } else {
                // Scenario: Within 30 days -> AUTO-REACTIVATE
                customer.setDeletedAt(null);
                userRepo.save(customer);
                // You might want to add a header or message indicating restoration
                System.out.println("Account reactivated for user: " + username);
            }
        }


        // 4. Proceed with standard JWT generation
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        final String jwt = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

        setCookie(response,"JWT_TOKEN",jwt,Duration.ofMinutes(30));
        setCookie(response,"REFRESH_TOKEN",refreshToken.getToken(),Duration.ofDays(7));

//        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", jwt)
//                .httpOnly(true)
//                .secure(true) // true in production
//                .path("/")
//                .maxAge(Duration.ofMinutes(30))
//                .sameSite("None")
//                .build();
//
//        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
//        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
//
//        // Add a "reactivated" flag in response if you want frontend to show a "Welcome Back" toast
        boolean wasReactivated = (customer.getDeletedAt() != null);
//
        return ResponseEntity.ok(Map.of(
                "username", username,
                "role", userDetails.getAuthorities(),
                "message", "Login successful",
                "activated",wasReactivated
        ));
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response){
        String refreshTokenStr = getCookievalue(request,"REFRESH_TOKEN");
        if (refreshTokenStr == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Refresh token is missing"));
        }
        try {
            RefreshToken refreshToken = refreshTokenService.rotateRefreshToken(refreshTokenStr);
            Customer user = refreshToken.getCustomer();

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String newJwt =jwtUtil.generateToken(userDetails);

            setCookie(response,"JWT_TOKEN",newJwt,Duration.ofMinutes(30));
            setCookie(response,"REFRESH_TOKEN",refreshToken.getToken(),Duration.ofDays(7));
            return ResponseEntity.ok(Map.of("message", "Token refreshed successfully"));
        }
        catch (Exception e){
            setCookie(response, "JWT_TOKEN", "", Duration.ZERO);
            setCookie(response, "REFRESH_TOKEN", "", Duration.ZERO);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }
    // REGISTER: create user and set cookie + Authorization header (same pattern)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        String username = req.get("username");
        String rawPassword = req.get("password");
        String rawEmail = req.get("email");
        String rawRole = req.get("role");

        // prevent creating ADMIN accidentally
        if ("ADMIN".equals(rawRole)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message","Please provide correct details"));
        }

        if (username == null || rawPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "username and password are required"));
        }
        Optional<Customer> existing = userRepo.findByEmail(rawEmail);
        if(existing.isPresent()){
            Customer u = existing.get();
            if(u.getVerified()){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered"));
            }
            else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "User exists but not verified. Please verify your account using the OTP sent to your email."));
            }
        }

        if (userRepo.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Username already exists"));
        }


        // create user with encoded password
        Customer user = new Customer();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword)); // BCrypt
        user.setRole(rawRole);
        user.setEmail(rawEmail);
        user.setVerified(false);
        userRepo.save(user);

        otpService.createAndSendOtpForMail(user.getId(),rawEmail,username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Verification code sent", "emailMasked", mask(rawEmail)));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body,HttpServletResponse response){
        String email = body.get("email");
        String otp = body.get("otp");

        if (otp == null) return ResponseEntity.badRequest().body(Map.of("message", "otp required"));

        try {
            otpService.verifyOtp(email, otp);
            Customer user = userRepo.findByEmail(email).orElseThrow();
            user.setVerified(true);
            userRepo.save(user);
            // load user details and generate JWT so client can use it immediately
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
            setCookie(response,"JWT_TOKEN",token,Duration.ofMinutes(30));
            setCookie(response,"REFRESH_TOKEN",refreshToken.getToken(),Duration.ofDays(7));
            // set cookie + header (same as login)
//            ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", token)
//                    .httpOnly(true)
//                    .secure(true) // true for prod
//                    .path("/")
//                    .maxAge(Duration.ofMinutes(30))
//                    .sameSite("None")
//                    .build();

//            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
//            response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
//                    "token", token,
                    "username", user.getUsername(),
                    "role",user.getRole()
            ));

        } catch (IllegalStateException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));

        }

    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
//        String username = body.get("username");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("message", "email required"));
        Optional<Customer> uOpt = userRepo.findByEmail(email);
        if (uOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No such user"));

        if (!otpService.canResend(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "Resend cooldown active"));
        }

        // create+send OTP
        otpService.createAndSendOtpForMail(uOpt.get().getId(), email,uOpt.get().getUsername());
        return ResponseEntity.ok(Map.of("message", "Code resent", "emailMasked", mask(email)));
    }

    @PutMapping("/update")
    public ResponseEntity<String> update(Principal principal, @RequestBody Map<String, String> req) {
        String email = req.get("email");
        String username = principal.getName();
        String password = req.get("password");

        Optional<Customer> customer = userRepo.findByUsername(username);
        if (!customer.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        Customer customer2 = customer.get();
        if (email != null) {
            customer2.setEmail(email);
        }
        if (password != null) {
            customer2.setPassword(passwordEncoder.encode(password)); // BCrypt
        }
        userRepo.save(customer2);
        return ResponseEntity.accepted().body("Profile Updated");
    }
    @PatchMapping("updateRole")
    public ResponseEntity<String> updateRole(Principal principal,@RequestBody Map<String, String> req){
        String role = req.get("role");
        String username = principal.getName();
        if(!role.equals("ADMIN")){
            return new ResponseEntity<>("Bad Credentials",HttpStatus.BAD_REQUEST);
        }
         Optional<Customer> customer = userRepo.findByUsername(username);
        if(customer.isPresent()) {
            Customer customer1 = customer.get();
            customer1.setRole(role);
            userRepo.save(customer1);
        }
        else{
            return new ResponseEntity<>("User Not Found",HttpStatus.NOT_FOUND);

        }
         return new ResponseEntity<>("Updated",HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response,Principal principal){
        if (principal != null){
            Customer c = userRepo.findByUsername(principal.getName()).orElse(null);
            if (c!=null){
                refreshTokenService.deleteByUserId(c.getId());
            }
        }
        SecurityContextHolder.clearContext();

        setCookie(response,"JWT_TOKEN","",Duration.ZERO);
        setCookie(response,"REFRESH_TOKEN","",Duration.ZERO);
//        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "")
//                .httpOnly(true)
//                .secure(true) // set according to your env (false for local http)
//                .path("/")
//                .maxAge(0)    // expire immediately
//                .sameSite("None") // set to "None" or "None" per your environment
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }


    private String mask(String email) {
        int at = email.indexOf("@");
        if (at <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePassRequest request,Principal principal,HttpServletResponse response){
        if(principal == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","authenticated required"));
        }
        String username = principal.getName();
        try {
            customerService.changePasswordForAuthenticatedUser(username,request.getOldPassword(),request.getNewPassword());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getNewPassword())
            );
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            final String jwt = jwtUtil.generateToken(userDetails);
            // If you use HttpSession (stateful): invalidate session(s) here if you want.
            ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", jwt)
                    .httpOnly(true)
                    .secure(true) // change to true in production (HTTPS)
                    .path("/")
                    .maxAge(Duration.ofMinutes(30))
                    .sameSite("None") // use None if frontend runs on different origin
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            // Optional compatibility header — remove in strict cookie-only setups
            response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
            // If you use JWTs: update passwordChangedAt (done in service) and ensure your JWT validation checks it.

            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            // log unexpected error (do not log passwords)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not update password"));
    }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody Map<String,String> req) {
        String email = req.get("email");
        otpService.createAndSendPasswordResetOtp(email);
        // generic response
        return ResponseEntity.ok(Map.of("message", "One Time Password for reset password is sent to your registered email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody Map<String ,String> req) {
        String email = req.get("email");
        String otp = req.get("otp");
        String password = req.get("password");

        try {
            boolean ok = otpService.verifyPasswordResetOtp(email,otp,password);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired token"));
            }
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please log in."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not reset password"));
        }
    }
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(Principal principal, HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }

        String username = principal.getName();
        Optional<Customer> customerOpt = userRepo.findByUsername(username);

        if (customerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        Customer customer = customerOpt.get();

        // Soft Delete: Set timestamp
        customer.setDeletedAt(Instant.now());
        userRepo.save(customer);

        // Logout user (Clear Cookies)
        SecurityContextHolder.clearContext();
        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("message", "Account deleted. You can restore it by logging in within 30 days."));
    }


    @PatchMapping("/complete-oauth-registration")
    public ResponseEntity<?> completeOAuthRegistration(Principal principal, @RequestBody Map<String, String> body,HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();
        String requestedRole = body.get("role"); // "STUDENT" or "INSTRUCTOR"

        // Validate Role Input
        if (!"STUDENT".equalsIgnoreCase(requestedRole) && !"INSTRUCTOR".equalsIgnoreCase(requestedRole) && !"USER".equalsIgnoreCase(requestedRole)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role. Choose STUDENT or INSTRUCTOR."));
        }

        Customer customer = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Security Check: Only allow this if they are currently PENDING
        // This prevents existing students from hacking themselves into Instructors
        if (!"PENDING".equals(customer.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Profile already setup. Cannot change role."));
        }

        // Update Role
        customer.setRole(requestedRole.toUpperCase());
        userRepo.save(customer);

        // RE-ISSUE TOKEN
        // The old token had "ROLE_PENDING". We need to give them a new token with "ROLE_STUDENT"
        UserDetails userDetails = userDetailsService.loadUserByUsername(customer.getUsername());
        String newToken = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);
        setCookie(response,"JWT_TOKEN",newToken,Duration.ofMinutes(30));
        setCookie(response,"REFRESH_TOKEN",refreshToken.getToken(),Duration.ofDays(7));

//        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", newToken)
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(Duration.ofMinutes(30))
//                .sameSite("None")
//                .build();
        boolean wasReactivated = (customer.getDeletedAt() != null);

        return ResponseEntity.ok()
                .body(Map.of(
                "username", username,
                "role", customer.getRole(),
                "message", "Registration successful",
                "activated",wasReactivated
        ));
    }
    // AuthController.java

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();
        Customer user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Return the same structure your frontend expects (UserInfo)
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole(), // Ensure format matches your JWT
                "email", user.getEmail()
        ));
    }

    private void setCookie(HttpServletResponse response, String name, String token, Duration maxAge){
        ResponseCookie cookie = ResponseCookie.from(name,token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE,cookie.toString());
    }
    private String getCookievalue(HttpServletRequest request, String name){
        if (request.getCookies() !=null){
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()){
                if (name.equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
