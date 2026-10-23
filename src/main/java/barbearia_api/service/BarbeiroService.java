package barbearia_api.service;

import barbearia_api.entity.Barbeiro;
import barbearia_api.exception.ResourceNotFoundException;
import barbearia_api.exception.OperacaoInvalidaException;
import barbearia_api.repository.BarbeiroRepository;
import barbearia_api.repository.AgendamentoRepository;
import barbearia_api.entity.StatusAgendamento;
import barbearia_api.config.ExpedienteConfig;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

@Service
public class BarbeiroService {
  private final BarbeiroRepository repository;
  private final AgendamentoRepository agendamentoRepository;
  private final ExpedienteConfig expedienteConfig;
  private final long duracaoMinutos;

  public BarbeiroService(BarbeiroRepository repository, AgendamentoRepository agendamentoRepository,
      ExpedienteConfig expedienteConfig,
      @Value("${api.agendamento.duracao-minutos:30}") long duracaoMinutos){
    this.repository = repository;
    this.agendamentoRepository = agendamentoRepository;
    this.expedienteConfig = expedienteConfig;
    this.duracaoMinutos = duracaoMinutos;
  }

  public Barbeiro salvar(Barbeiro barbeiro){
    return repository.save(barbeiro);
  }

  public List<Barbeiro> listarTodos(){
    return repository.findByAtivoTrueOrderByNomeAsc();
  }

  public Barbeiro buscarPorId(Long id){
    return repository.findById(id).filter(Barbeiro::isAtivo)
            .orElseThrow(()-> new ResourceNotFoundException("Barbeiro", id));
  }

  public List<LocalDateTime> horariosDisponiveis(Long id, LocalDate data) {
    buscarPorId(id);
    String faixa = expedienteConfig.getHorarios().get(data.getDayOfWeek());
    if (faixa == null || faixa.isBlank()) return List.of();

    String[] limites = faixa.split("-", -1);
    if (limites.length != 2 || duracaoMinutos <= 0) {
      throw new IllegalStateException("Expediente ou duracao de agendamento invalido.");
    }
    LocalTime abertura = LocalTime.parse(limites[0]);
    LocalTime fechamento = LocalTime.parse(limites[1]);
    List<LocalDateTime> disponiveis = new java.util.ArrayList<>();
    LocalDateTime agora = LocalDateTime.now();
    for (LocalDateTime inicio = data.atTime(abertura);
         !inicio.plusMinutes(duracaoMinutos).toLocalTime().isAfter(fechamento);
         inicio = inicio.plusMinutes(duracaoMinutos)) {
      LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
      if (!inicio.isBefore(agora)
          && !agendamentoRepository.existsByBarbeiroIdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusNot(
              id, fim, inicio.minus(Duration.ofMinutes(duracaoMinutos)), StatusAgendamento.CANCELADO)) {
        disponiveis.add(inicio);
      }
    }
    return List.copyOf(disponiveis);
  }

  public Barbeiro atualizar(Long id, String nome, String telefone, String especialidade) {
    Barbeiro barbeiro = buscarPorId(id);
    barbeiro.setNome(nome);
    barbeiro.setTelefone(telefone);
    barbeiro.setEspecialidade(especialidade);
    return repository.save(barbeiro);
  }

  @Transactional
  public void deletar(Long id) {
    Barbeiro barbeiro = repository.findByIdForUpdate(id)
            .orElseThrow(()-> new ResourceNotFoundException("Barbeiro", id));
    if (!barbeiro.isAtivo()) return;
    if (agendamentoRepository.existsByBarbeiroIdAndStatus(id, StatusAgendamento.AGENDADO)) {
      throw new OperacaoInvalidaException(
          "Remaneje ou cancele os agendamentos ativos deste barbeiro antes de desativa-lo.");
    }
    // Desativar preserva o historico concluido/cancelado e impede novas reservas.
    barbeiro.setAtivo(false);
    repository.save(barbeiro);
  }

}
