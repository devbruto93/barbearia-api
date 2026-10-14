package barbearia_api.service;

import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import barbearia_api.dto.AgendamentoDTO;
import barbearia_api.dto.AgendamentoResponseDTO;
import barbearia_api.entity.Agendamento;
import barbearia_api.entity.Barbeiro;
import barbearia_api.entity.Cliente;
import barbearia_api.entity.StatusAgendamento;
import barbearia_api.exception.AcessoNegadoException;
import barbearia_api.exception.HorarioIndisponivelException;
import barbearia_api.exception.OperacaoInvalidaException;
import barbearia_api.exception.ResourceNotFoundException;
import barbearia_api.repository.AgendamentoRepository;
import barbearia_api.repository.BarbeiroRepository;
import barbearia_api.repository.ClienteRepository;

@Service
public class AgendamentoService {

  private final AgendamentoRepository repository;
  private final ClienteRepository clienteRepository;
  private final BarbeiroRepository barbeiroRepository;
  private final long duracaoAgendamentoMinutos;

  public AgendamentoService(AgendamentoRepository repository,
      ClienteRepository clienteRepository,
      BarbeiroRepository barbeiroRepository,
      @Value("${api.agendamento.duracao-minutos:30}") long duracaoAgendamentoMinutos) {
    this.repository = repository;
    this.clienteRepository = clienteRepository;
    this.barbeiroRepository = barbeiroRepository;
    if (duracaoAgendamentoMinutos <= 0) {
      throw new IllegalArgumentException("A duracao do agendamento deve ser positiva.");
    }
    this.duracaoAgendamentoMinutos = duracaoAgendamentoMinutos;
  }

  @Transactional
  public AgendamentoResponseDTO criar(AgendamentoDTO dto, String emailAutenticado, boolean admin) {
    Cliente cliente = resolverCliente(dto.clienteId(), emailAutenticado, admin);

    Barbeiro barbeiro = barbeiroRepository.findById(dto.barbeiroId())
        .orElseThrow(() -> new ResourceNotFoundException("Barbeiro", dto.barbeiroId()));

    LocalDateTime inicio = dto.dataHora();
    LocalDateTime fim = inicio.plus(Duration.ofMinutes(duracaoAgendamentoMinutos));
    if (repository.existsByBarbeiroIdAndDataHoraLessThanAndDataHoraGreaterThanAndStatusNot(
        barbeiro.getId(), fim, inicio.minus(Duration.ofMinutes(duracaoAgendamentoMinutos)),
        StatusAgendamento.CANCELADO)) {
      throw new HorarioIndisponivelException();
    }

    Agendamento agendamento = new Agendamento();
    agendamento.setCliente(cliente);
    agendamento.setBarbeiro(barbeiro);
    agendamento.setDataHora(dto.dataHora());
    agendamento.setStatus(StatusAgendamento.AGENDADO);

    return paraResposta(repository.save(agendamento));
  }

  @Transactional(readOnly = true)
  public List<AgendamentoResponseDTO> listarTodos() {
    return repository.findAllByOrderByDataHoraDesc()
        .stream()
        .map(this::paraResposta)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AgendamentoResponseDTO> listarDoCliente(String emailAutenticado) {
    Cliente cliente = buscarClientePorEmail(emailAutenticado);

    return repository.findByClienteIdOrderByDataHoraDesc(cliente.getId())
        .stream()
        .map(this::paraResposta)
        .toList();
  }

  @Transactional(readOnly = true)
  public AgendamentoResponseDTO buscarPorId(Long id) {
    return paraResposta(buscarEntidade(id));
  }

  @Transactional
  public AgendamentoResponseDTO cancelar(Long id, String emailAutenticado, boolean admin) {
    Agendamento agendamento = buscarEntidade(id);

    boolean dono = agendamento.getCliente().getUsuario().getEmail().equals(emailAutenticado);
    if (!admin && !dono) {
      throw new AcessoNegadoException("Voce so pode cancelar os seus proprios agendamentos.");
    }

    if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
      throw new OperacaoInvalidaException(
          "Apenas agendamentos com status AGENDADO podem ser cancelados.");
    }

    agendamento.setStatus(StatusAgendamento.CANCELADO);
    return paraResposta(repository.save(agendamento));
  }

  @Transactional
  public AgendamentoResponseDTO concluir(Long id) {
    Agendamento agendamento = buscarEntidade(id);

    if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
      throw new OperacaoInvalidaException(
          "Apenas agendamentos com status AGENDADO podem ser concluidos.");
    }

    agendamento.setStatus(StatusAgendamento.CONCLUIDO);
    return paraResposta(repository.save(agendamento));
  }

  /**
   * Um CLIENTE sempre age sobre o proprio cadastro; o clienteId do corpo so
   * e considerado quando quem chama e ADMIN.
   */
  private Cliente resolverCliente(Long clienteId, String emailAutenticado, boolean admin) {
    if (admin && clienteId != null) {
      return clienteRepository.findById(clienteId)
          .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
    }
    return buscarClientePorEmail(emailAutenticado);
  }

  private Cliente buscarClientePorEmail(String email) {
    return clienteRepository.findByUsuarioEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Cliente nao encontrado para o usuario autenticado."));
  }

  private Agendamento buscarEntidade(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Agendamento", id));
  }

  private AgendamentoResponseDTO paraResposta(Agendamento agendamento) {
    return new AgendamentoResponseDTO(
        agendamento.getId(),
        agendamento.getCliente().getId(),
        agendamento.getCliente().getNome(),
        agendamento.getBarbeiro().getId(),
        agendamento.getBarbeiro().getNome(),
        agendamento.getDataHora(),
        agendamento.getStatus().name());
  }
}
