package barbearia_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import barbearia_api.entity.Agendamento;
import barbearia_api.entity.StatusAgendamento;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByClienteIdOrderByDataHoraDesc(Long clienteId);

    List<Agendamento> findAllByOrderByDataHoraDesc();

    boolean existsByBarbeiroIdAndStatus(Long barbeiroId, StatusAgendamento status);

    List<Agendamento> findByBarbeiroIdAndStatusOrderByDataHoraAsc(
            Long barbeiroId, StatusAgendamento status);

    List<Agendamento> findByBarbeiroIdAndStatusAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
            Long barbeiroId, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

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

    @Modifying
    @Transactional
    @Query("delete from Agendamento a where a.status in :statuses and a.dataHora < :limite")
    int removerHistoricoExpirado(@Param("statuses") List<StatusAgendamento> statuses,
            @Param("limite") LocalDateTime limite);
}
