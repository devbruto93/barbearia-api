package barbearia_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres-123456";
    private static final String OUTRO_SEGREDO = "segredo-diferente-tambem-acima-de-32-caracteres-abc";
    private static final long UMA_HORA_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SEGREDO, UMA_HORA_MS);
    }

    @Test
    @DisplayName("token gerado carrega email e role e volta identico na leitura")
    void roundTrip() {
        String token = jwtService.gerarToken("joao@email.com", "CLIENTE");

        assertThat(jwtService.extrairEmail(token)).isEqualTo("joao@email.com");
        assertThat(jwtService.extrairRole(token)).isEqualTo("CLIENTE");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("token assinado com outro segredo e rejeitado")
    void tokenAssinadoComOutroSegredo() {
        JwtService outro = new JwtService(OUTRO_SEGREDO, UMA_HORA_MS);
        String tokenDeOutro = outro.gerarToken("joao@email.com", "CLIENTE");

        assertThat(jwtService.isTokenValid(tokenDeOutro)).isFalse();
    }

    @Test
    @DisplayName("token expirado e rejeitado")
    void tokenExpirado() {
        JwtService expirado = new JwtService(SEGREDO, -1_000L);
        String token = expirado.gerarToken("joao@email.com", "CLIENTE");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("lixo no lugar do token e rejeitado sem estourar excecao")
    void tokenMalformado() {
        assertThat(jwtService.isTokenValid("isto-nao-e-um-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("extrairEmail lanca excecao em token invalido, o filtro trata isso")
    void extrairEmailComTokenInvalido() {
        assertThatThrownBy(() -> jwtService.extrairEmail("token-adulterado"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("segredo curto demais impede a aplicacao de subir")
    void segredoCurtoDemais() {
        assertThatThrownBy(() -> new JwtService("curto", UMA_HORA_MS))
                .isInstanceOf(WeakKeyException.class);
    }
}
