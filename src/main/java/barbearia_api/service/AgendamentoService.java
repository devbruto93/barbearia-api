package barbearia_api.service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.LocalDateTime;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import barbearia_api.dto.AgendamentoDTO;
import barbearia_api.dto.AgendamentoResponseDTO;
import barbearia_api.dto.DecisaoAlteracaoHorarioDTO.Decisao;
import barbearia_api.dto.DecisaoAlteracaoHorarioDTO;
import barbearia_api.config.ExpedienteConfig;
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
  private final ExpedienteConfig expedienteConfig;
  private final long duracaoAgendamentoMinutos;

  public AgendamentoService(AgendamentoRepository repository,
      ClienteRepository clienteRepository,
      BarbeiroRepository barbeiroRepository,
      ExpedienteConfig expedienteConfig,
      @Value("${api.agendamento.duracao-minutos:30}") long duracaoAgendamentoMinutos) {
    this.repository = repository;
    this.clienteRepository = clienteRepository;
    this.barbeiroRepository = barbeiroRepository;
    this.expedienteConfig = expedienteConfig;
    if (duracaoAgendamentoMinutos <= 0) {
      throw new IllegalArgumentException("A duracao do agendamento deve ser positiva.");
    }
    this.duracaoAgendamentoMinutos = duracaoAgendamentoMinutos;
  }

  @Transactional
  public AgendamentoResponseDTO criar(AgendamentoDTO dto, String emailAutenticado, boolean admin) {
    Cliente cliente = resolverCliente(dto.clienteId(), emailAutenticado, admin);

    // Serializa reservas do mesmo barbeiro durante a transacao, incluindo
    // checagem de conflito e INSERT.
    Barbeiro barbeiro = travarBarbeiros(dto.barbeiroId()).get(dto.barbeiroId());

    LocalDateTime inicio = dto.dataHora();
    LocalDateTime fim = inicio.plusMinutes(duracaoAgendamentoMinutos);
    if (!expedienteConfig.permite(inicio, duracaoAgendamentoMinutos)) {
      throw new HorarioIndisponivelException(
          "O horario solicitado esta fora do expediente da barbearia.");
    }
    if (temConflito(barbeiro.getId(), inicio, duracaoAgendamentoMinutos, null)) {
      throw new HorarioIndisponivelException();
    }

    Agendamento agendamento = new Agendamento();
    agendamento.setCliente(cliente);
    agendamento.setBarbeiro(barbeiro);
    agendamento.setDataHora(dto.dataHora());
    agendamento.setDuracaoMinutos(Math.toIntExact(duracaoAgendamentoMinutos));
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
  public AgendamentoResponseDTO realocar(Long id, AgendamentoDTO dto) {
    Agendamento agendamento = buscarEntidade(id);
    if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
      throw new OperacaoInvalidaException(
          "Apenas agendamentos com status AGENDADO podem ser realocados.");
    }
    if (dto.clienteId() == null) {
      throw new OperacaoInvalidaException(
          "clienteId e obrigatorio para realocar um agendamento.");
    }

    Cliente cliente = resolverCliente(dto.clienteId(), null, true);
    Barbeiro barbeiro = travarBarbeiros(
        agendamento.getBarbeiro().getId(), dto.barbeiroId()).get(dto.barbeiroId());
    LocalDateTime inicio = dto.dataHora();
    long duracao = agendamento.getDuracaoMinutos();
    if (!expedienteConfig.permite(inicio, duracao)) {
      throw new HorarioIndisponivelException(
          "O horario solicitado esta fora do expediente da barbearia.");
    }
    if (temConflito(barbeiro.getId(), inicio, duracao, id)) {
      throw new HorarioIndisponivelException();
    }

    agendamento.setCliente(cliente);
    agendamento.setBarbeiro(barbeiro);
    agendamento.setDataHora(inicio);
    agendamento.setHorarioOriginal(null);
    agendamento.setAlteracaoHorarioPendente(false);
    agendamento.setRemarcacaoSolicitada(false);
    return paraResposta(repository.save(agendamento));
  }

  /** Acrescenta duracao ao atendimento e desloca a sequencia seguinte do mesmo dia. */
  @Transactional
  public List<AgendamentoResponseDTO> adicionarTempo(Long id, int minutos) {
    if (minutos < 1 || minutos > 240) {
      throw new OperacaoInvalidaException("O tempo adicional deve estar entre 1 e 240 minutos.");
    }
    Agendamento alvo = buscarEntidade(id);
    if (alvo.getStatus() != StatusAgendamento.AGENDADO) {
      throw new OperacaoInvalidaException("So e possivel alterar um agendamento ativo.");
    }

    Long barbeiroId = alvo.getBarbeiro().getId();
    travarBarbeiros(barbeiroId);

    LocalDateTime limiteAntecedencia = LocalDateTime.now().plusHours(2);
    if (alvo.getDataHora().isBefore(limiteAntecedencia)) {
      throw new OperacaoInvalidaException(
          "A alteracao precisa ser feita com pelo menos duas horas de antecedencia.");
    }

    int novaDuracao = Math.addExact(alvo.getDuracaoMinutos(), minutos);
    if (!expedienteConfig.permite(alvo.getDataHora(), novaDuracao)) {
      throw new HorarioIndisponivelException(
          "O tempo adicional ultrapassa o horario de fechamento da barbearia.");
    }

    LocalDate data = alvo.getDataHora().toLocalDate();
    List<Agendamento> doDia = repository
        .findByBarbeiroIdAndStatusAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraAsc(
            barbeiroId, StatusAgendamento.AGENDADO, data.atStartOfDay(), data.plusDays(1).atStartOfDay());
    List<Agendamento> deslocados = doDia.stream()
        .filter(a -> a.getDataHora().isAfter(alvo.getDataHora()))
        .toList();

    if (alvo.isAlteracaoHorarioPendente()
        || deslocados.stream().anyMatch(Agendamento::isAlteracaoHorarioPendente)) {
      throw new OperacaoInvalidaException(
          "Aguarde os clientes responderem aos avisos de horario anteriores antes de fazer outra alteracao.");
    }

    for (Agendamento proximo : deslocados) {
      if (proximo.getDataHora().isBefore(limiteAntecedencia)) {
        throw new OperacaoInvalidaException(
            "A alteracao precisa ser feita com pelo menos duas horas de antecedencia ao proximo cliente.");
      }
      LocalDateTime novoHorario = proximo.getDataHora().plusMinutes(minutos);
      if (!expedienteConfig.permite(novoHorario, proximo.getDuracaoMinutos())) {
        throw new HorarioIndisponivelException(
            "A sequencia remanejada ultrapassaria o expediente. Ajuste os horarios manualmente.");
      }
    }

    // As mudancas e os avisos sao gravados na mesma transacao, sob o lock do barbeiro.
    alvo.setDuracaoMinutos(novaDuracao);
    List<Agendamento> alterados = new java.util.ArrayList<>();
    alterados.add(alvo);
    for (Agendamento proximo : deslocados) {
      if (proximo.getHorarioOriginal() == null) {
        proximo.setHorarioOriginal(proximo.getDataHora());
      }
      proximo.setDataHora(proximo.getDataHora().plusMinutes(minutos));
      proximo.setAlteracaoHorarioPendente(true);
      proximo.setRemarcacaoSolicitada(false);
      alterados.add(proximo);
    }
    return repository.saveAll(alterados).stream().map(this::paraResposta).toList();
  }

  @Transactional
  public AgendamentoResponseDTO responderAlteracaoHorario(
      Long id, String email, DecisaoAlteracaoHorarioDTO resposta) {
    Agendamento agendamento = buscarEntidade(id);
    if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
      throw new OperacaoInvalidaException("So e possivel responder a alteracoes de agendamentos ativos.");
    }
    if (!agendamento.getCliente().getUsuario().getEmail().equals(email)) {
      throw new AcessoNegadoException("Voce so pode responder aos avisos dos seus agendamentos.");
    }
    Map<Long, Barbeiro> barbeirosTravados = travarBarbeiros(
        agendamento.getBarbeiro().getId(), resposta.barbeiroId());
    if (!agendamento.isAlteracaoHorarioPendente()) {
      throw new OperacaoInvalidaException("Este agendamento nao tem alteracao de horario pendente.");
    }
    if (resposta.decisao() == Decisao.REMARCAR) {
      if ((resposta.barbeiroId() == null) != (resposta.dataHora() == null)) {
        throw new OperacaoInvalidaException(
            "Para remarcar agora, informe juntos o barbeiroId e a dataHora; ou envie ambos vazios para solicitar ajuda.");
      }
      if (resposta.dataHora() != null) {
        Barbeiro novoBarbeiro = barbeirosTravados.get(resposta.barbeiroId());
        if (!expedienteConfig.permite(resposta.dataHora(), agendamento.getDuracaoMinutos())) {
          throw new HorarioIndisponivelException("O novo horario esta fora do expediente.");
        }
        if (temConflito(novoBarbeiro.getId(), resposta.dataHora(),
            agendamento.getDuracaoMinutos(), id)) {
          throw new HorarioIndisponivelException();
        }
        agendamento.setBarbeiro(novoBarbeiro);
        agendamento.setDataHora(resposta.dataHora());
        agendamento.setHorarioOriginal(null);
        agendamento.setAlteracaoHorarioPendente(false);
        agendamento.setRemarcacaoSolicitada(false);
        return paraResposta(repository.save(agendamento));
      }
      agendamento.setRemarcacaoSolicitada(true);
    } else {
      if (resposta.barbeiroId() != null || resposta.dataHora() != null) {
        throw new OperacaoInvalidaException("A opcao aceitar nao recebe um novo horario.");
      }
      if (agendamento.isRemarcacaoSolicitada()) {
        throw new OperacaoInvalidaException(
            "A remarcacao ja foi solicitada; aguarde a barbearia ajustar o horario.");
      }
      agendamento.setAlteracaoHorarioPendente(false);
      agendamento.setHorarioOriginal(null);
    }
    return paraResposta(repository.save(agendamento));
  }

  @Transactional
  public AgendamentoResponseDTO cancelar(Long id, String emailAutenticado, boolean admin) {
    Agendamento agendamento = buscarEntidade(id);
    travarBarbeiros(agendamento.getBarbeiro().getId());

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
    travarBarbeiros(agendamento.getBarbeiro().getId());

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
      Cliente cliente = clienteRepository.findById(clienteId)
          .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
      if (cliente.getUsuario() == null) {
        throw new OperacaoInvalidaException(
            "O cliente precisa estar vinculado a um usuario para receber agendamentos.");
      }
      return cliente;
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

  private boolean temConflito(Long barbeiroId, LocalDateTime inicio, long duracao, Long ignorarId) {
    LocalDateTime fim = inicio.plusMinutes(duracao);
    return repository.findByBarbeiroIdAndStatusOrderByDataHoraAsc(
            barbeiroId, StatusAgendamento.AGENDADO).stream()
        .filter(a -> ignorarId == null || !a.getId().equals(ignorarId))
        .anyMatch(a -> a.getDataHora().isBefore(fim)
            && a.getDataHora().plusMinutes(a.getDuracaoMinutos()).isAfter(inicio));
  }

  /**
   * Todas as alteracoes de agenda travam os registros de barbeiro em ordem crescente.
   * Isso serializa reservas concorrentes e reduz o risco de deadlock ao mover entre barbeiros.
   */
  private Map<Long, Barbeiro> travarBarbeiros(Long... ids) {
    Map<Long, Barbeiro> travados = new TreeMap<>();
    java.util.Arrays.stream(ids)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .sorted()
        .forEach(id -> {
          Barbeiro barbeiro = barbeiroRepository.findByIdForUpdate(id)
              .orElseThrow(() -> new ResourceNotFoundException("Barbeiro", id));
          if (!barbeiro.isAtivo()) throw new ResourceNotFoundException("Barbeiro", id);
          travados.put(id, barbeiro);
        });
    return travados;
  }

  private AgendamentoResponseDTO paraResposta(Agendamento agendamento) {
    String aviso = null;
    if (agendamento.isAlteracaoHorarioPendente()) {
      aviso = agendamento.isRemarcacaoSolicitada()
          ? "A barbearia alterou seu horario para " + agendamento.getDataHora()
              + ", e seu pedido de remarcacao esta aguardando atendimento."
          : "Seu horario foi alterado de " + agendamento.getHorarioOriginal() + " para "
              + agendamento.getDataHora() + ". Aceite a alteracao ou solicite uma remarcacao.";
    }
    return new AgendamentoResponseDTO(
        agendamento.getId(),
        agendamento.getCliente().getId(),
        agendamento.getCliente().getNome(),
        agendamento.getBarbeiro().getId(),
        agendamento.getBarbeiro().getNome(),
        agendamento.getDataHora(),
        agendamento.getStatus().name(),
        agendamento.getDuracaoMinutos(),
        agendamento.getHorarioOriginal(),
        agendamento.isAlteracaoHorarioPendente(),
        agendamento.isRemarcacaoSolicitada(),
        aviso);
  }
}
