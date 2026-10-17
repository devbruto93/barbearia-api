package barbearia_api.dto;

import java.util.List;

public record ExportacaoDadosDTO(
        String email,
        String nome,
        String telefone,
        List<AgendamentoResponseDTO> agendamentos
) { }
