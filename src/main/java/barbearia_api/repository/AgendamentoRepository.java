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
     * Verifica sobreposicao com um intervalo [inicio, fim). Agendamentos
     * CANCELADOS nao ocupam horario; dois atendimentos consecutivos podem
     * comecar exatamente quando o anterior termina.
     */
    boolean existsByBarbeiroIdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusNot(
            Long barbeiroId, LocalDateTime fim, LocalDateTime inicio,
            StatusAgendamento status);

    boolean existsByBarbeiroIdAndIdNotAndDataHoraLessThanAndDataHoraGreaterThanAndStatusNot(
            Long barbeiroId, Long id, LocalDateTime fim, LocalDateTime inicio,
            StatusAgendamento status);
}
