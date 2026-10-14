package barbearia_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import barbearia_api.entity.Agendamento;
import barbearia_api.entity.StatusAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(Long clienteId);

    List<Agendamento> findAllByOrderByDataHoraDesc();

    /**
     * Verifica se o barbeiro ja tem um horario ocupado no mesmo instante.
     * Agendamentos CANCELADOS sao ignorados, porque o cancelamento devolve
     * o horario para a agenda.
     */
    boolean existsByBarbeiroIdAndDataHoraAndStatusNot(
            Long barbeiroId, LocalDateTime dataHora, StatusAgendamento status);
}
