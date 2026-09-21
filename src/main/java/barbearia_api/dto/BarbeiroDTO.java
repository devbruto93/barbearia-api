package barbearia_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BarbeiroDTO{

	@NotBlank(message = "Nome é obrigatório")
	@Size(min = 3, max = 100,
				message = "Nome deve ter entre 3 e 100 caracteres")
	private String nome;

	@NotBlank(message = "Telefone é obrigatório")
	@Pattern(
			regexp = "^[0-9]{10,11}$",
			message = "Telefone deve conter 10 ou 11 números")
	private String telefone;

	@NotBlank(message = "Especialidade é obrigatória")
	@Size(min = 3, max = 50,
				message = "Especialidade deve ter entre 3 e 50 caracteres")
	private String especialidade;

	public BarbeiroDTO(){
	}

	public BarbeiroDTO(String nome, String telefone, String especialidade){
		this.nome = nome;
		this.telefone = telefone;
		this.especialidade = especialidade;
	}

	public String getNome(){
		return nome;
	}

	public void setNome(String nome){
		this.nome = nome;
	}

	public String getTelefone(){
		return telefone;
	}

	public void setTelefone(String telefone){
		this.telefone = telefone;
	}

	public String getEspecialidade(){
		return especialidade;
	}

	public void setEspecialidade(String especialidade){
		this.especialidade = especialidade;
	}
}