package barbearia_api.controller;

import barbearia_api.entity.Barbeiro;
import barbearia_api.service.BarbeiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import barbearia_api.dto.BarbeiroDTO;
import barbearia_api.dto.BarbeiroResponseDTO;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
public class BarbeiroController {

	private final BarbeiroService service;

	public BarbeiroController(BarbeiroService service){
		this.service = service;
	}

	@PostMapping
	public BarbeiroResponseDTO cadastrar(
		@Valid @RequestBody BarbeiroDTO dto){

		Barbeiro barbeiro = new Barbeiro();
		barbeiro.setNome(dto.getNome());
		barbeiro.setTelefone(dto.getTelefone());
		barbeiro.setEspecialidade(dto.getEspecialidade());

		return BarbeiroResponseDTO.de(service.salvar(barbeiro));
	}

	@GetMapping
	public List<BarbeiroResponseDTO> listarTodos(){
		return service.listarTodos().stream().map(BarbeiroResponseDTO::de).toList();
	}

	@GetMapping("/{id}")
	public BarbeiroResponseDTO buscarPorId(@PathVariable Long id){
		return BarbeiroResponseDTO.de(service.buscarPorId(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
