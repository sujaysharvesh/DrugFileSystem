package com.example.DrugSecure.userAuth.User.security;


import com.example.DrugSecure.userAuth.User.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY_STRING;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());
    }

    public String GenerateToken(User user){
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails){
        return extractUserName(token).equals(userDetails.getUsername());
    }

    public String extractUserName(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)  // Validate the token with the secret key
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("username", String.class);  // ✅ Get "username" from claims
    }


    public String ExtractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if(authHeader != null || authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public long getExpirationTime(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey) // Use Key, not String
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().getTime(); // Returns expiration in milliseconds
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token is expired");
        } catch (JwtException e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }
}
