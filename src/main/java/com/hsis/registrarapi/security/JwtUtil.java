package com.hsis.registrarapi.security;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import java.security.Key;
import java.util.Date;
import java.util.List;
@Component
public class JwtUtil {

  


    private final String SECRET_STRING;
    private final Key SECRET_KEY; // Keep your existing type (Key or SecretKey)

    public JwtUtil(@Value("${jwt.secret}") String secretString) {
        this.SECRET_STRING = secretString;
        this.SECRET_KEY = Keys.hmacShaKeyFor(secretString.getBytes());
    }
    

    // Keep all your existing methods below unchanged
    // 2. The Expiration Time (How long the ID card is valid. 1000ms * 60s * 60m * 24h = 24 hours)
    private final long EXPIRATION_TIME = 86400000;

    // 3. The ID Card Printer (Generates the token)
    public String generateToken(String email , List<String> roles ,String fullName) {
        return Jwts.builder()
                .claim("roles",roles)
                .claim("fullName", fullName)
                .setSubject(email) // The payload (who this card belongs to)
                .setIssuedAt(new Date()) // The time it was printed
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // When it expires
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256) // The cryptographic lock
                .compact(); // Squishes it all into a single String
    }

   // 4. Extract the Email from the token
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 6. Extract the custom "roles" claim
    public List<String> extractRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
        // Extract the "roles" key and cast it back into a List
        return claims.get("roles", List.class);
    }
   
    // 5. Verify the cryptographic signature and check expiration
    public boolean validateToken(String token) {
        try {
            // If this line executes without crashing, the token is 100% authentic and not expired
            Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // If the math breaks, it means it's forged, tampered with, or expired
            return false;
        }
    }
}

