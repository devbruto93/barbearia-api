package barbearia_api.controller;

import barbearia_api.entity.Cliente;
import barbearia_api.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import barbearia_api.dto.ClienteDTO;
import barbearia_api.dto.ClienteResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service){
		this.service = service;
	}

	@GetMapping("/me")
	public ClienteResponseDTO meuPerfil(@AuthenticationPrincipal UserDetails usuario){
		return ClienteResponseDTO.de(service.buscarPorEmailDoUsuario(usuario.getUsername()));
	}

	@PutMapping("/me")
	public ClienteResponseDTO atualizarMeuPerfil(@AuthenticationPrincipal UserDetails usuario,
			@Valid @RequestBody ClienteDTO dto){
		return ClienteResponseDTO.de(service.atualizarPorEmailDoUsuario(
				usuario.getUsername(), dto.getNome(), dto.getTelefone()));
	}

	@GetMapping
	public List<ClienteResponseDTO> listarTodos(){
		return service.listarTodos().stream().map(ClienteResponseDTO::de).toList();
	}

	@GetMapping("/{id:\\d+}")
	public ClienteResponseDTO buscarPorId(@PathVariable Long id){
		return ClienteResponseDTO.de(service.buscarPorId(id));
	}

	@PutMapping("/{id:\\d+}")
	public ClienteResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto){
		return ClienteResponseDTO.de(service.atualizar(id, dto.getNome(), dto.getTelefone()));
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
