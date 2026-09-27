package barbearia_api.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import barbearia_api.entity.Usuario;
import barbearia_api.repository.UsuarioRepository;

/**
 * Carrega o usuario do banco para o Spring Security.
 *
 * O nome do usuario aqui e o email, e nao um id ou username separado: e o mesmo
 * valor que vai no subject do JWT, o que mantem o filtro e o banco falando a
 * mesma lingua.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public CustomUserDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getSenha())
                .roles(usuario.getRole().name())
                .build();
    }
}
