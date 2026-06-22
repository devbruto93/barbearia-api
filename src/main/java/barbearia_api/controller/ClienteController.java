package barbearia_api.controller;

import barbearia_api.entity.Cliente;
import barbearia_api.service.ClienteService;
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

	@GetMapping
	public List<Cliente> listarTodos(){
		return service.listarTodos();
	}

	@GetMapping("/{id}")
	public Cliente buscarPorId(@PathVariable Long id){
		return service.buscarPorId(id);
	}

	@DeleteMapping("/{id}")
	public void deletar(@PathVariable Long id){
		service.deletar(id);
	}
}