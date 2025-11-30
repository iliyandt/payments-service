package demos.springdata.paymentservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);


    @PostConstruct
    public void init() {
        LOGGER.info("=== SECURITY DEBUG START ===");
        if (secretKey == null || secretKey.isEmpty()) {
            LOGGER.error("!!! FATAL: SECRET KEY IS NULL OR EMPTY !!!");
        } else {
            String maskedKey = secretKey.length() > 10 ? secretKey.substring(0, 6) + "..." : "Too short";
            LOGGER.info("SUCCESS: Secret Key loaded. Length: {}", secretKey.length());
            LOGGER.info("Key Preview: {}", maskedKey);
        }
        LOGGER.info("=== SECURITY DEBUG END ===");
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
