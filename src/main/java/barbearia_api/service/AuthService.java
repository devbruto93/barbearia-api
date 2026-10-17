package barbearia_api.service;

import java.time.Instant;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import barbearia_api.dto.CadastroDTO;
import barbearia_api.dto.LoginDTO;
import barbearia_api.dto.TokenResponseDTO;
import barbearia_api.entity.Cliente;
import barbearia_api.entity.Role;
import barbearia_api.entity.Usuario;
import barbearia_api.exception.CredenciaisInvalidasException;
import barbearia_api.exception.ContaBloqueadaException;
import barbearia_api.exception.EmailJaCadastradoException;
import barbearia_api.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository repository;
    private final ClienteService clienteService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final long expiration;
    private final long lockoutMinutes;

    public AuthService(UsuarioRepository repository, ClienteService clienteService,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager,
            @Value("${api.security.jwt.expiration}") long expiration,
            @Value("${api.security.login.lockout-minutes:15}") long lockoutMinutes) {
        this.repository = repository;
        this.clienteService = clienteService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.expiration = expiration;
        this.lockoutMinutes = lockoutMinutes;
    }

    /**
     * O role e fixado em CLIENTE e nunca lido do corpo da requisicao.
     *
     * Aceitar o role do cliente permitiria que qualquer um se cadastrasse como
     * ADMIN, entao a promocao a admin tem que ser um fluxo separado e protegido.
     *
     * O nome informado vira o Cliente deste usuario. O telefone nasce vazio:
     * varios cadastros sem telefone sao validos, e a unicidade so barra valor
     * repetido quando ele existe. O cliente preenche o telefone em /clientes/me.
     */
    @Transactional
    public TokenResponseDTO cadastrar(CadastroDTO dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setRole(Role.CLIENTE);

        Usuario salvo = repository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setNome(dto.nome());
        cliente.setUsuario(salvo);
        clienteService.salvar(cliente);

        return gerarResposta(salvo);
    }

    public TokenResponseDTO login(LoginDTO dto) {
        repository.liberarBloqueioExpirado(dto.email(), Instant.now());
        Usuario conta = repository.findByEmail(dto.email()).orElse(null);
        if (conta != null && conta.isLoginBloqueado()) {
            throw new ContaBloqueadaException();
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        } catch (AuthenticationException e) {
            repository.registrarFalhaLogin(dto.email());
            if (repository.bloquearAposTerceiraFalha(dto.email(),
                    Instant.now().plus(Duration.ofMinutes(lockoutMinutes))) > 0) {
                throw new ContaBloqueadaException();
            }
            throw new CredenciaisInvalidasException();
        }

        Usuario usuario = repository.findByEmail(authentication.getName())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (repository.limparFalhasLogin(usuario.getEmail()) == 0) {
            throw new ContaBloqueadaException();
        }

        return gerarResposta(usuario);
    }

    @Transactional
    public void desbloquearLogin(String email) {
        if (repository.desbloquearLogin(email) == 0) {
            throw new barbearia_api.exception.ResourceNotFoundException("Usuario nao encontrado.");
        }
    }

    private TokenResponseDTO gerarResposta(Usuario usuario) {
        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name(),
                usuario.getTokenVersion());

        return new TokenResponseDTO(
            token,
            usuario.getEmail(),
            usuario.getRole().name(),
            Instant.now().plusMillis(expiration)
        );
    }

    public void logout(String email) {
        repository.revogarTokens(email);
    }
}
