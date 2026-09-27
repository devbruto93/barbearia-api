package barbearia_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDTO(

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    String senha
) {
}
