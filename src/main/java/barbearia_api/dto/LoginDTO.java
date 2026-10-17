package barbearia_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    @Size(max = 72, message = "Senha deve ter no maximo 72 caracteres")
    String senha
) {
}
