package barbearia_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(unique = true)
  private String telefone;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @JoinColumn(name = "usuario_id", unique = true)
  private Usuario usuario;

  public Cliente(){
  }

  public Cliente(Long id, String nome, String telefone){
    this.id = id;
    this.nome = nome;
    this.telefone = telefone;
  }

  public Long getId(){
    return id;
  }
  public void setId(Long id){
    this.id = id;
  }

  public String getNome(){
    return nome;
  }
  public void setNome(String nome){
    this.nome = nome;
  }

  public String getTelefone(){
    return telefone;
  }
  public void setTelefone(String telefone){
    this.telefone = telefone;
  }

  @JsonIgnore
  public Usuario getUsuario(){
    return usuario;
  }
  public void setUsuario(Usuario usuario){
    this.usuario = usuario;
  }

}
