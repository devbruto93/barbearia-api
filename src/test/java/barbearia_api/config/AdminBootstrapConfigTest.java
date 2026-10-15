package barbearia_api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.repository.UsuarioRepository;

@SpringBootTest
class AdminBootstrapConfigTest {

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AdminBootstrapConfig config = new AdminBootstrapConfig();

    @BeforeEach
    void limparUsuarios() {
        usuarios.deleteAll();
    }

    @Test
    void criaAdminComEmailNormalizadoESenhaCriptografada() {
        config.criarAdminSeConfigurado(usuarios, passwordEncoder,
                " Admin@Exemplo.com ", "senha-inicial-segura-123");

        Usuario criado = usuarios.findByEmail("admin@exemplo.com").orElseThrow();
        assertThat(criado.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("senha-inicial-segura-123", criado.getSenha())).isTrue();
    }

    @Test
    void adminJaExistenteNaoEAlterado() {
        Usuario existente = salvarUsuario("admin@exemplo.com", Role.ADMIN);
        existente.setSenha(passwordEncoder.encode("senha-ja-existente"));
        usuarios.save(existente);

        config.criarAdminSeConfigurado(usuarios, passwordEncoder,
                "admin@exemplo.com", "senha-inicial-segura-123");

        assertThat(passwordEncoder.matches("senha-ja-existente",
                usuarios.findByEmail("admin@exemplo.com").orElseThrow().getSenha())).isTrue();
    }

    @Test
    void naoPromoveClienteExistente() {
        salvarUsuario("admin@exemplo.com", Role.CLIENTE);

        assertThatThrownBy(() -> config.criarAdminSeConfigurado(usuarios, passwordEncoder,
                "admin@exemplo.com", "senha-inicial-segura-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pertence a um usuario CLIENTE");
        assertThat(usuarios.findByEmail("admin@exemplo.com").orElseThrow().getRole())
                .isEqualTo(Role.CLIENTE);
    }

    @Test
    void naoCriaAdminSemCredenciaisERejeitaSenhaCurta() {
        config.criarAdminSeConfigurado(usuarios, passwordEncoder, "", "");
        assertThat(usuarios.findAll()).isEmpty();

        assertThatThrownBy(() -> config.criarAdminSeConfigurado(usuarios, passwordEncoder,
                "admin@exemplo.com", "curta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pelo menos 12 caracteres");
        assertThat(usuarios.findAll()).isEmpty();
    }

    private Usuario salvarUsuario(String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha-qualquer"));
        usuario.setRole(role);
        return usuarios.save(usuario);
    }
}
