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

@Entity
@Table(name = "agendamentos")
public class Agendamento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  public StatusAgendamento getStatus(){
    return status;
  }
  public void setStatus(StatusAgendamento status){
    this.status = status;
  }
}
