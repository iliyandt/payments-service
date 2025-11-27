package demos.springdata.paymentservice.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@ExtendWith(MockitoExtension.class)
public class JwtServiceUTest {

    @InjectMocks
    private JwtService jwtService;

    private final String TEST_SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";


    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET_KEY);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername_WhenTokenIsValid() {
        String expectedUsername = "admin@gym.bg";
        String token = generateTestToken(expectedUsername);

        String actualUsername = jwtService.extractUsername(token);

        Assertions.assertEquals(expectedUsername, actualUsername);
    }


    @Test
    void extractUsername_ShouldThrow_WhenTokenIsExpired() {
        String username = "user@gym.bg";

        String expiredToken = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();

        Assertions.assertThrows(ExpiredJwtException.class, () -> {
            jwtService.extractUsername(expiredToken);
        });
    }


    @Test
    void extractUsername_ShouldThrowException_WhenTokenIsTampered() {

        String validToken = generateTestToken("hacker");

        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

        Assertions.assertThrows(Exception.class, () -> {
            jwtService.extractUsername(tamperedToken);
        });
    }

    private String generateTestToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
