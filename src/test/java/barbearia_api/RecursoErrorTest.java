package barbearia_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import barbearia_api.entity.Cliente;
import barbearia_api.repository.ClienteRepository;
import barbearia_api.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RecursoErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        repository.deleteAll();
        usuarioRepository.deleteAll();
        token = cadastrarCliente();
    }

    @Test
    @DisplayName("GET inexistente devolve 404 com JSON, nao 500")
    void getInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/clientes/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Cliente nao encontrado: id 999999"));
    }

    @Test
    @DisplayName("DELETE inexistente devolve 404 em vez de 500")
    void deleteInexistenteDevolve404() throws Exception {
        mockMvc.perform(delete("/clientes/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("telefone repetido devolve 409 em vez de 500")
    void telefoneRepetidoDevolve409() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Fulano");
        cliente.setTelefone("11988887777");
        repository.save(cliente);

        mockMvc.perform(post("/clientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Outro","telefone":"11988887777"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ja existe um registro com esse dado."));
    }

    @Test
    @DisplayName("DELETE existente devolve 204")
    void deleteExistenteDevolve204() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Fulano");
        cliente.setTelefone("11988887777");
        repository.save(cliente);

        mockMvc.perform(delete("/clientes/" + cliente.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private String cadastrarCliente() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"erro@email.com","senha":"senha123","nome":"Teste"}
                            """))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }
}
