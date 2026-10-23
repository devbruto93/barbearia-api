package barbearia_api.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "agendamentos")
public class Agendamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Detecta gravacoes concorrentes do mesmo agendamento, alem do lock por barbeiro. */
  @Version
  @Column(nullable = false)
  private long versao;

  /**
   * Apagar o cliente apaga seus agendamentos no banco (ON DELETE CASCADE).
   * Sem isso, deletar um cliente com historico estouraria violacao de chave
   * estrangeira e viraria um 409 sem explicacao util.
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "barbeiro_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Barbeiro barbeiro;

  @Column(nullable = false)
  private LocalDateTime dataHora;

  @Column(name = "duracao_minutos", nullable = false)
  private int duracaoMinutos = 30;

  @Column(name = "horario_original")
  private LocalDateTime horarioOriginal;

  @Column(name = "alteracao_horario_pendente", nullable = false)
  private boolean alteracaoHorarioPendente;

  @Column(name = "remarcacao_solicitada", nullable = false)
  private boolean remarcacaoSolicitada;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StatusAgendamento status;

  public Agendamento(){
  }

  public Long getId(){
    return id;
  }
  public void setId(Long id){
    this.id = id;
  }

  public Cliente getCliente(){
    return cliente;
  }
  public void setCliente(Cliente cliente){
    this.cliente = cliente;
  }

  public Barbeiro getBarbeiro(){
    return barbeiro;
  }
  public void setBarbeiro(Barbeiro barbeiro){
    this.barbeiro = barbeiro;
  }

  public LocalDateTime getDataHora(){
    return dataHora;
  }
  public void setDataHora(LocalDateTime dataHora){
    this.dataHora = dataHora;
  }

  public int getDuracaoMinutos(){ return duracaoMinutos; }
  public void setDuracaoMinutos(int duracaoMinutos){ this.duracaoMinutos = duracaoMinutos; }
  public LocalDateTime getHorarioOriginal(){ return horarioOriginal; }
  public void setHorarioOriginal(LocalDateTime horarioOriginal){ this.horarioOriginal = horarioOriginal; }
  public boolean isAlteracaoHorarioPendente(){ return alteracaoHorarioPendente; }
  public void setAlteracaoHorarioPendente(boolean pendente){ this.alteracaoHorarioPendente = pendente; }
  public boolean isRemarcacaoSolicitada(){ return remarcacaoSolicitada; }
  public void setRemarcacaoSolicitada(boolean solicitada){ this.remarcacaoSolicitada = solicitada; }

  public StatusAgendamento getStatus(){
    return status;
  }
  public void setStatus(StatusAgendamento status){
    this.status = status;
  }
}
