package barbearia_api.dto;

import java.time.LocalDateTime;

/**
 * Projeçao de um agendamento para o cliente da API.
 *
 * As entidades Cliente e Barbeiro nao sao expostas diretamente: devolver as
 * associacoes completas arrastaria o Usuario do cliente para o JSON. Aqui so
 * vao o id e o nome de cada lado, que e o que a tela de agenda precisa.
 */
public record AgendamentoResponseDTO(
    Long id,
    Long clienteId,
    String clienteNome,
    Long barbeiroId,
    String barbeiroNome,
    LocalDateTime dataHora,
    String status,
    int duracaoMinutos,
    LocalDateTime horarioAnterior,
    boolean alteracaoHorarioPendente,
    boolean remarcacaoSolicitada,
    String avisoAlteracaoHorario
) {
}
