package barbearia_api.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.repository.UsuarioRepository;

/** Cria o primeiro administrador somente quando credenciais iniciais sao configuradas. */
@Configuration
public class AdminBootstrapConfig {

    @Bean
    ApplicationRunner bootstrapAdmin(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String senha) {
        return args -> criarAdminSeConfigurado(usuarios, passwordEncoder, email, senha);
    }

    @Transactional
    void criarAdminSeConfigurado(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
            String email, String senha) {
        boolean emailInformado = email != null && !email.isBlank();
        boolean senhaInformada = senha != null && !senha.isBlank();
        if (!emailInformado && !senhaInformada) {
            return;
        }
        if (!emailInformado || !senhaInformada || senha.length() < 12) {
            throw new IllegalStateException(
                    "Configure BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD; "
                    + "a senha inicial deve ter pelo menos 12 caracteres.");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        var existente = usuarios.findByEmail(emailNormalizado);
        if (existente.isPresent()) {
            if (existente.get().getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "O email de bootstrap ja pertence a um usuario CLIENTE; "
                        + "use um email exclusivo para o primeiro ADMIN.");
            }
            return;
        }

        Usuario admin = new Usuario();
        admin.setEmail(emailNormalizado);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN);
        usuarios.save(admin);
    }
}
