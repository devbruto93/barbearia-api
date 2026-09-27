package barbearia_api.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expiration;

    /**
     * A chave e derivada uma unica vez, na construcao. Se o segredo for curto
     * demais, o WeakKeyException estoura aqui e a aplicacao nem sobe, em vez de
     * falhar no meio de uma requisição.
     */
    public JwtService(@Value("${api.security.jwt.secret}") String secret,
            @Value("${api.security.jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String gerarToken(String email, String role) {
        Date now = new Date();

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extrairEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
