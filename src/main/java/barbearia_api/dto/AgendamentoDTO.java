package barbearia_api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de criacao ou realocacao de um agendamento.
 *
 * clienteId e opcional e so tem efeito quando quem chama e ADMIN, permitindo
 * agendar em nome de um cliente. Um CLIENTE autenticado sempre agenda para si
 * mesmo, mesmo que envie este campo: o servidor ignora o valor e usa o cliente
 * ligado ao token, para que ninguem agende no nome de outra pessoa. Na
 * realocacao, operacao exclusiva do ADMIN, clienteId precisa ser informado.
 */
public record AgendamentoDTO(

    @NotNull(message = "Barbeiro e obrigatorio")
    Long barbeiroId,

    @NotNull(message = "Data e hora sao obrigatorias")
    @Future(message = "A data e hora devem estar no futuro")
    LocalDateTime dataHora,

    Long clienteId
) {
}
