package barbearia_api.controller;

import barbearia_api.entity.Cliente;
import barbearia_api.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import barbearia_api.dto.ClienteDTO;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service){
		this.service = service;
	}

	@PostMapping
	public Cliente cadastrar(
		@Valid @RequestBody ClienteDTO dto){

		Cliente cliente = new Cliente();
		cliente.setNome(dto.getNome());
		cliente.setTelefone(dto.getTelefone());

		return service.salvar(cliente);
	}

	@GetMapping("/me")
	public Cliente meuPerfil(@AuthenticationPrincipal UserDetails usuario){
		return service.buscarPorEmailDoUsuario(usuario.getUsername());
	}

	@PutMapping("/me")
	public Cliente atualizarMeuPerfil(@AuthenticationPrincipal UserDetails usuario,
			@Valid @RequestBody ClienteDTO dto){
		return service.atualizarPorEmailDoUsuario(
				usuario.getUsername(), dto.getNome(), dto.getTelefone());
	}

	@GetMapping
	public List<Cliente> listarTodos(){
		return service.listarTodos();
	}

	@GetMapping("/{id:\\d+}")
	public Cliente buscarPorId(@PathVariable Long id){
		return service.buscarPorId(id);
	}

	@PutMapping("/{id:\\d+}")
	public Cliente atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto){
		return service.atualizar(id, dto.getNome(), dto.getTelefone());
	}

	@DeleteMapping("/{id:\\d+}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
