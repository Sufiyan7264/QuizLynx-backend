package com.projectJava.quizApp.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JWTUtil {

    @Value("${jwt.secret:}")
    private String base64Secret;

    private Key key;
    private final long jwtExpirationMs = 30*60*1000L;

    @PostConstruct
    public void init(){
        if(base64Secret != null && !base64Secret.isBlank()){
            byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        else{
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    public String generateToken(UserDetails userDetails){
        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
        if(authority.startsWith("ROLE_")){
            authority = authority.substring(5);
        }
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("role",authority)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(System.currentTimeMillis()+jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    private Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    public Date extractIssuedAt(String token){
        return extractClaim(token,Claims::getIssuedAt);
    }
    public String extractUsername(String token){
        return extractClaim(token,Claims::getSubject);
    }
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp.before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    public Key getKey() {
        return key;
    }
//
//    private Key key;
//    private final long jwtExpirationMs = 30 * 60 * 1000L; // 30 minutes
//
//    @PostConstruct
//    public void init() {
//        if (base64Secret != null && !base64Secret.isBlank()) {
//            byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
//            this.key = Keys.hmacShaKeyFor(keyBytes);
//        } else {
//            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
//        }
//    }
//
//    /**
//     * Generate JWT token using userDetails. Supports both regular and OAuth2 users.
//     */
//    public String generateToken(UserDetails userDetails) {
//        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
//        if (authority.startsWith("ROLE_")) {
//            authority = authority.substring(5);
//        }
//        return Jwts.builder()
//                .setSubject(userDetails.getUsername())
//                .claim("role", authority)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
//                .signWith(key)
//                .compact();
//    }
//
//    /**
//     * Generate token with additional claims (optional for OAuth2 scenarios).
//     */
//    public String generateToken(UserDetails userDetails, Map<String, Object> additionalClaims) {
//        String authority = userDetails.getAuthorities().iterator().next().getAuthority();
//        if (authority.startsWith("ROLE_")) {
//            authority = authority.substring(5);
//        }
//        return Jwts.builder()
//                .setSubject(userDetails.getUsername())
//                .claim("role", authority)
//                .addClaims(additionalClaims) // add more details like provider if needed
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
//                .signWith(key)
//                .compact();
//    }
//
//    public String extractUsername(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//
//    public String extractRole(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .get("role", String.class);
//    }
//
//    private Date extractExpiration(String token) {
//        return Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody()
//                .getExpiration();
//    }
//
//    private boolean isTokenExpired(String token) {
//        Date exp = extractExpiration(token);
//        return exp.before(new Date());
//    }
//
//    public boolean validateToken(String token, UserDetails userDetails) {
//        try {
//            final String username = extractUsername(token);
//            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    public Key getKey() {
//        return key;
//    }


}
