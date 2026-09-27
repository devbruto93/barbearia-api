package barbearia_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.repository.UsuarioRepository;

/**
 * Percorre a cadeia de seguranca inteira: cadastro, login, e o que cada role
 * consegue ou nao consegue acessar. O filtro JWT so pode ser verificado de
 * ponta a ponta, porque um token valido que nao existe no banco tem de dar 401
 * e nao 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("cadastro devolve 201 e token com role CLIENTE")
    void cadastroDevolveTokenDeCliente() throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"joao@email.com","senha":"senha123","nome":"Joao Silva"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("CLIENTE"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty());
    }

    @Test
    @DisplayName("a senha nunca e devolvida nem gravada em texto puro")
    void senhaNuncaExpoeTextoPuro() throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"joao@email.com","senha":"senha123","nome":"Joao Silva"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senha").doesNotExist());

        String senhaNoBanco = repository.findByEmail("joao@email.com").orElseThrow().getSenha();

        org.assertj.core.api.Assertions.assertThat(senhaNoBanco)
                .isNotEqualTo("senha123")
                .startsWith("$2");
    }

    @Test
    @DisplayName("email repetido devolve 409")
    void emailRepetidoDevolve409() throws Exception {
        cadastrar("joao@email.com");

        mockMvc.perform(post("/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"joao@email.com","senha":"outra123","nome":"Joao"}
                    """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe um usuario cadastrado com este email."));
    }

    @Test
    @DisplayName("senha errada devolve 401, sem distinguir usuario de senha")
    void senhaErradaDevolve401() throws Exception {
        cadastrar("joao@email.com");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"joao@email.com","senha":"errada"}
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("email inexistente devolve 401 com a mesma mensagem")
    void emailInexistenteDevolve401() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"ninguem@email.com","senha":"qualquer1"}
                    """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou senha invalidos."));
    }

    @Test
    @DisplayName("payload invalido devolve 400 apontando os campos")
    void payloadInvalidoDevolve400() throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nao-e-email","senha":"123","nome":"Jo"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").value("Email deve ser valido"))
                .andExpect(jsonPath("$.campos.senha").isNotEmpty());
    }

    @Test
    @DisplayName("sem token devolve 401")
    void semTokenDevolve401() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token com assinatura falsificada devolve 401")
    void tokenFalsificadoDevolve401() throws Exception {
        String token = cadastrar("joao@email.com");
        String falsificado = token.substring(0, token.lastIndexOf('.') + 1) + "invalida";

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + falsificado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("header sem o prefixo Bearer e ignorado")
    void headerSemPrefixoBearerEOu() throws Exception {
        String token = cadastrar("joao@email.com");

        mockMvc.perform(get("/clientes").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("cliente autenticado acessa /clientes com 200")
    void clienteAcessaClientes() throws Exception {
        String token = cadastrar("joao@email.com");

        mockMvc.perform(get("/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cliente autenticado recebe 403 em /barbeiros")
    void clienteNaoAcessaBarbeiros() throws Exception {
        String token = cadastrar("joao@email.com");

        mockMvc.perform(get("/barbeiros").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("admin promoted no banco acessa /barbeiros com 200")
    void adminAcessaBarbeiros() throws Exception {
        cadastrar("admin@email.com");

        Usuario admin = repository.findByEmail("admin@email.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        repository.save(admin);

        String token = login("admin@email.com", "senha123");

        mockMvc.perform(get("/barbeiros").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token de admin nao pode ser rebaixado editando o proprio payload")
    void tokenComRoleForjadaERejeitado() throws Exception {
        String token = cadastrar("maria@email.com");

        String payload = new String(java.util.Base64.getUrlDecoder().decode(
                token.split("\\.")[1])).replace("\"CLIENTE\"", "\"ADMIN\"");

        String forjado = token.split("\\.")[0] + "."
                + java.util.Base64.getUrlEncoder().encodeToString(payload.getBytes())
                + "." + token.split("\\.")[2];

        mockMvc.perform(get("/barbeiros").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    private String cadastrar(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","senha":"senha123","nome":"Teste"}
                            """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        return lerToken(result);
    }

    private String login(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","senha":"%s"}
                            """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();

        return lerToken(result);
    }

    private String lerToken(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }
}
