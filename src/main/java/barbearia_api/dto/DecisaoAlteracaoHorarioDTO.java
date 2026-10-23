package barbearia_api.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record DecisaoAlteracaoHorarioDTO(
        @NotNull Decisao decisao,
        Long barbeiroId,
        @Future(message = "O novo horario deve estar no futuro") LocalDateTime dataHora) {
    public enum Decisao { ACEITAR, REMARCAR }
}
