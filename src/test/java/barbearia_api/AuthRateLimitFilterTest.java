package barbearia_api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import barbearia_api.security.AuthRateLimitFilter;
import tools.jackson.databind.ObjectMapper;

class AuthRateLimitFilterTest {
    @Test
    void limitaTentativasPorEnderecoEDevolveRetryAfter() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(new ObjectMapper(), 1, 60, 5, 600, false);
        MockHttpServletResponse primeiraResposta = new MockHttpServletResponse();
        filter.doFilter(request(), primeiraResposta, new MockFilterChain());
        assertThat(primeiraResposta.getStatus()).isEqualTo(200);

        MockHttpServletResponse segundaResposta = new MockHttpServletResponse();
        filter.doFilter(request(), segundaResposta, new MockFilterChain());
        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(segundaResposta.getHeader("Retry-After")).isNotNull();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
