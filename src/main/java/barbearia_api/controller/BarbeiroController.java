package barbearia_api.controller;

import barbearia_api.entity.Barbeiro;
import barbearia_api.service.BarbeiroService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import barbearia_api.dto.BarbeiroDTO;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
public class BarbeiroController {

	private final BarbeiroService service;

	public BarbeiroController(BarbeiroService service){
		this.service = service;
	}

	@PostMapping
	public Barbeiro cadastrar(
		@Valid @RequestBody BarbeiroDTO dto){

		Barbeiro barbeiro = new Barbeiro();
		barbeiro.setNome(dto.getNome());
		barbeiro.setTelefone(dto.getTelefone());
		barbeiro.setEspecialidade(dto.getEspecialidade());

		return service.salvar(barbeiro);
	}

	@GetMapping
	public List<Barbeiro> listarTodos(){
		return service.listarTodos();
	}

	@GetMapping("/{id}")
	public Barbeiro buscarPorId(@PathVariable Long id){
		return service.buscarPorId(id);
	}

	@DeleteMapping("/{id}")
	public void deletar(@PathVariable Long id){
		service.deletar(id);
	}
}