package com.example.demo.security;

import com.example.demo.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String ROL_CLAIM = "rol";

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.parser = Jwts.parser().verifyWith(signingKey).build();
        this.expirationMs = properties.expiration();
    }

    public String generateToken(UserPrincipal user) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(ROL_CLAIM, user.getRol())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parser.parseSignedClaims(token).getPayload());
    }
}
