package barbearia_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import barbearia_api.entity.Barbeiro;
import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.repository.AgendamentoRepository;
import barbearia_api.repository.BarbeiroRepository;
import barbearia_api.repository.ClienteRepository;
import barbearia_api.repository.UsuarioRepository;

/**
 * Percorre o ciclo de vida de um agendamento pela cadeia de seguranca: quem
 * cria, quem enxerga, quem cancela e quem conclui. As regras de horario e de
 * transicao de status so podem ser verificadas de ponta a ponta, porque
 * dependem do banco e do token ao mesmo tempo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AgendamentoFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BarbeiroRepository barbeiroRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBase() {
        agendamentoRepository.deleteAll();
        clienteRepository.deleteAll();
        barbeiroRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("cliente cria agendamento e recebe 201 com status AGENDADO")
    void clienteCriaAgendamento() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String token = cadastrar("joao@email.com");

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
                            """.formatted(barbeiro.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.barbeiroNome").value("Carlos"))
                .andExpect(jsonPath("$.clienteNome").value("Teste"))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    @DisplayName("data no passado devolve 400 apontando o campo")
    void dataNoPassadoDevolve400() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String token = cadastrar("joao@email.com");

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"barbeiroId":%d,"dataHora":"2000-01-01T10:00:00"}
                            """.formatted(barbeiro.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dataHora")
                        .value("A data e hora devem estar no futuro"));
    }

    @Test
    @DisplayName("barbeiro inexistente devolve 404")
    void barbeiroInexistenteDevolve404() throws Exception {
        String token = cadastrar("joao@email.com");

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"barbeiroId":999999,"dataHora":"2030-01-01T10:00:00"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("horario ja ocupado do barbeiro devolve 409")
    void horarioOcupadoDevolve409() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenMaria = cadastrar("maria@email.com");

        String corpo = """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId());

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + tokenJoao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("O barbeiro ja possui um agendamento neste horario."));
    }

    @Test
    @DisplayName("cancela um horario e libera o mesmo horario para outro agendamento")
    void cancelarLiberaOHorario() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenMaria = cadastrar("maria@email.com");

        String corpo = """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId());

        Long id = extrairId(criarAgendamento(tokenJoao, corpo));

        mockMvc.perform(patch("/agendamentos/" + id + "/cancelar")
                        .header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + tokenMaria)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("cliente lista apenas os proprios agendamentos em /me")
    void clienteListaApenasOsProprios() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenMaria = cadastrar("maria@email.com");

        criarAgendamento(tokenJoao, """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId()));

        mockMvc.perform(get("/agendamentos/me").header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clienteNome").value("Teste"));

        mockMvc.perform(get("/agendamentos/me").header("Authorization", "Bearer " + tokenMaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("cliente autenticado recebe 403 ao listar todos os agendamentos")
    void clienteNaoListaTodos() throws Exception {
        String token = cadastrar("joao@email.com");

        mockMvc.perform(get("/agendamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("cliente nao cancela agendamento de outro")
    void clienteNaoCancelaDeOutro() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenMaria = cadastrar("maria@email.com");

        Long id = extrairId(criarAgendamento(tokenJoao, """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId())));

        mockMvc.perform(patch("/agendamentos/" + id + "/cancelar")
                        .header("Authorization", "Bearer " + tokenMaria))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("cliente nao conclui agendamento, apenas admin")
    void clienteNaoConclui() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String token = cadastrar("joao@email.com");

        Long id = extrairId(criarAgendamento(token, """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId())));

        mockMvc.perform(patch("/agendamentos/" + id + "/concluir")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin lista todos e conclui um agendamento")
    void adminConclui() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenAdmin = promoverAdmin("admin@email.com");

        Long id = extrairId(criarAgendamento(tokenJoao, """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId())));

        mockMvc.perform(get("/agendamentos").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/agendamentos/" + id + "/concluir")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }

    @Test
    @DisplayName("cancelar agendamento ja concluido devolve 409")
    void cancelarJaConcluidoDevolve409() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        String tokenJoao = cadastrar("joao@email.com");
        String tokenAdmin = promoverAdmin("admin@email.com");

        Long id = extrairId(criarAgendamento(tokenJoao, """
            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00"}
            """.formatted(barbeiro.getId())));

        mockMvc.perform(patch("/agendamentos/" + id + "/concluir")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/agendamentos/" + id + "/cancelar")
                        .header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Apenas agendamentos com status AGENDADO podem ser cancelados."));
    }

    @Test
    @DisplayName("admin agenda em nome de um cliente informando clienteId")
    void adminAgendaEmNomeDeCliente() throws Exception {
        Barbeiro barbeiro = criarBarbeiro();
        cadastrar("joao@email.com");
        String tokenAdmin = promoverAdmin("admin@email.com");

        Long clienteId = clienteRepository.findByUsuarioEmail("joao@email.com").orElseThrow().getId();

        mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"barbeiroId":%d,"dataHora":"2030-01-01T10:00:00","clienteId":%d}
                            """.formatted(barbeiro.getId(), clienteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(clienteId))
                .andExpect(jsonPath("$.clienteNome").value("Teste"));
    }

    private Barbeiro criarBarbeiro() {
        return barbeiroRepository.save(new Barbeiro(null, "Carlos", "11999990000", "Degrade"));
    }

    private MvcResult criarAgendamento(String token, String corpo) throws Exception {
        return mockMvc.perform(post("/agendamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private Long extrairId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
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

    private String promoverAdmin(String email) throws Exception {
        cadastrar(email);

        Usuario admin = usuarioRepository.findByEmail(email).orElseThrow();
        admin.setRole(Role.ADMIN);
        usuarioRepository.save(admin);

        return login(email, "senha123");
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
