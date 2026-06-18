package barbearia_api.controller;

import barbearia_api.entity.Cliente;
import barbeariaapi.service.ClienteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service){
		this.service = service;
	}

	@PostMapping
	public List<Cliente> listarTodos(){
		return service.listarTodos();
	}

	@GetMapping
	pubic List<Cliente> listarTodos(){
		return service.listarTodos();
	}

	@GetMapping("/{id}")
	pubic Cliente buscarPorId(@PathVariable Long id){
		return service.buscarPorId(id);
	}

	@DeleteMapping("/{id}")
	public void deletar(@PathVariable Long id){
		service.deletar(id);
	}
}