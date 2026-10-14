package barbearia_api.entity;

/**
 * Ciclo de vida de um agendamento.
 *
 * AGENDADO: horario reservado, ainda pode ser cancelado ou concluido.
 * CONCLUIDO: atendimento realizado, estado final.
 * CANCELADO: horario liberado, estado final.
 *
 * Apenas o status CANCELADO libera o horario do barbeiro para um novo
 * agendamento no mesmo instante.
 */
public enum StatusAgendamento {
    AGENDADO,
    CONCLUIDO,
    CANCELADO
}
