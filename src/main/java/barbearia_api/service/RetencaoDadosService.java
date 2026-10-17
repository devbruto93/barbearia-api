package barbearia_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import barbearia_api.entity.StatusAgendamento;
import barbearia_api.repository.AgendamentoRepository;

/** Expurga apenas histórico encerrado quando o controlador configura um prazo. */
@Service
public class RetencaoDadosService {
    private final AgendamentoRepository repository;
    private final int diasRetencao;

    public RetencaoDadosService(AgendamentoRepository repository,
            @Value("${privacy.retention.appointments-days:0}") int diasRetencao) {
        this.repository = repository;
        this.diasRetencao = diasRetencao;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void removerHistoricoExpirado() {
        if (diasRetencao <= 0) return;
        repository.removerHistoricoExpirado(
                List.of(StatusAgendamento.CANCELADO, StatusAgendamento.CONCLUIDO),
                LocalDateTime.now().minusDays(diasRetencao));
    }
}
