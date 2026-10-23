package barbearia_api.controller;

import barbearia_api.entity.Barbeiro;
import barbearia_api.service.BarbeiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import barbearia_api.dto.BarbeiroDTO;
import barbearia_api.dto.BarbeiroResponseDTO;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

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

	@GetMapping("/{id}/disponibilidade")
	public List<LocalDateTime> horariosDisponiveis(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data){
		return service.horariosDisponiveis(id, data);
	}

	@GetMapping("/{id}")
	public BarbeiroResponseDTO buscarPorId(@PathVariable Long id){
		return BarbeiroResponseDTO.de(service.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public BarbeiroResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody BarbeiroDTO dto){
		return BarbeiroResponseDTO.de(service.atualizar(
				id, dto.getNome(), dto.getTelefone(), dto.getEspecialidade()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id){
		service.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
