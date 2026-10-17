package barbearia_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DesbloqueioContaDTO(@NotBlank @Email String email) { }
