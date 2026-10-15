package barbearia_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import barbearia_api.dto.AgendamentoDTO;
import barbearia_api.dto.AgendamentoResponseDTO;
import barbearia_api.service.AgendamentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

  private static final String ROLE_ADMIN = "ROLE_ADMIN";

  private final AgendamentoService service;

  public AgendamentoController(AgendamentoService service){
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<AgendamentoResponseDTO> criar(
      @AuthenticationPrincipal UserDetails usuario,
      @Valid @RequestBody AgendamentoDTO dto) {

    AgendamentoResponseDTO criado = service.criar(
        dto, usuario.getUsername(), isAdmin(usuario));

    return ResponseEntity.status(HttpStatus.CREATED).body(criado);
  }

  @GetMapping("/me")
  public List<AgendamentoResponseDTO> meus(@AuthenticationPrincipal UserDetails usuario){
    return service.listarDoCliente(usuario.getUsername());
  }

  @GetMapping
  public List<AgendamentoResponseDTO> listarTodos(){
    return service.listarTodos();
  }

  @GetMapping("/{id:\\d+}")
  public AgendamentoResponseDTO buscarPorId(@PathVariable Long id){
    return service.buscarPorId(id);
  }

  @PutMapping("/{id:\\d+}")
  public AgendamentoResponseDTO realocar(@PathVariable Long id,
      @Valid @RequestBody AgendamentoDTO dto){
    return service.realocar(id, dto);
  }

  @PatchMapping("/{id:\\d+}/cancelar")
  public AgendamentoResponseDTO cancelar(@AuthenticationPrincipal UserDetails usuario,
      @PathVariable Long id){
    return service.cancelar(id, usuario.getUsername(), isAdmin(usuario));
  }

  @PatchMapping("/{id:\\d+}/concluir")
  public AgendamentoResponseDTO concluir(@PathVariable Long id){
    return service.concluir(id);
  }

  private boolean isAdmin(UserDetails usuario){
    return usuario.getAuthorities().stream()
        .anyMatch(autoridade -> ROLE_ADMIN.equals(autoridade.getAuthority()));
  }
}
