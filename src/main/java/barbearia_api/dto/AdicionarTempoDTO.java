package barbearia_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdicionarTempoDTO(
        @Min(value = 1, message = "Informe pelo menos um minuto adicional")
        @Max(value = 240, message = "O limite por alteracao e de 240 minutos")
        int minutos) {
}
