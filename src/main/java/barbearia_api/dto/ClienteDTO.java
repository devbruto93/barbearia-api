package barbearia_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteDTO{


	@NotBlank(message = "Nome é obrigatório")
	@Size(min = 3, max = 100,
				message = "Nome de ter entre 3 e 100 caracteres")
	private String nome;

	@NotBlank(message = "Telefone é obrigatório")
	@Pattern(
			regexp = "^[0-9]{10,11}$",
			message = "Telefone deve conter 10 ou 11 números")
	private String telefone;

	public ClienteDTO(){
	}

	public ClienteDTO(String nome, String telefone){
		this.nome = nome;
		this.telefone = telefone;
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
}