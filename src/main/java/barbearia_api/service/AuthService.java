package barbearia_api.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import barbearia_api.dto.CadastroDTO;
import barbearia_api.dto.LoginDTO;
import barbearia_api.dto.TokenResponseDTO;
import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.exception.CredenciaisInvalidasException;
import barbearia_api.exception.EmailJaCadastradoException;
import barbearia_api.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long expiration;

    public AuthService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
            JwtService jwtService, AuthenticationManager authenticationManager,
            @Value("${api.security.jwt.expiration}") long expiration) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.expiration = expiration;
    }

    /**
     * O role e fixado em CLIENTE e nunca lido do corpo da requisicao.
     *
     * Aceitar o role do cliente permitiria que qualquer um se cadastrasse como
     * ADMIN, entao a promocao a admin tem que ser um fluxo separado e protegido.
     */
    public TokenResponseDTO cadastrar(CadastroDTO dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setRole(Role.CLIENTE);

        repository.save(usuario);

        return gerarResposta(usuario);
    }

    public TokenResponseDTO login(LoginDTO dto) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        } catch (AuthenticationException e) {
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = repository.findByEmail(authentication.getName())
                .orElseThrow(CredenciaisInvalidasException::new);

        return gerarResposta(usuario);
    }

    private TokenResponseDTO gerarResposta(Usuario usuario) {
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name());

        return new TokenResponseDTO(
            token,
            usuario.getEmail(),
            usuario.getRole().name(),
            Instant.now().plusMillis(expiration)
        );
    }
}
