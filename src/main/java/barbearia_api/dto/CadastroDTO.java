package barbearia_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroDTO(

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
    String email,

    @NotBlank(message = "Senha e obrigatoria")
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,72}$",
        message = "Senha deve conter numero, letra maiuscula, letra minuscula e caractere especial")
    String senha,

    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    String nome
) {
}
