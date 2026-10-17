package barbearia_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import barbearia_api.dto.CadastroDTO;
import barbearia_api.dto.LoginDTO;
import barbearia_api.dto.TokenResponseDTO;
import barbearia_api.dto.DesbloqueioContaDTO;
import barbearia_api.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<TokenResponseDTO> cadastrar(@Valid @RequestBody CadastroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    /** Suporte usa esta rota apos confirmar a identidade do cliente por WhatsApp. */
    @PostMapping("/admin/desbloqueios")
    public ResponseEntity<Void> desbloquear(@Valid @RequestBody DesbloqueioContaDTO dto) {
        service.desbloquearLogin(dto.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails usuario) {
        service.logout(usuario.getUsername());
        return ResponseEntity.noContent().build();
    }
}
