package barbearia_api.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "barbeiros")
public class Barbeiro {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String telefone;

  @Column(nullable = false)
  private String especialidade;

  public Barbeiro(){
  }

  public Barbeiro(Long id, String nome, String telefone, String especialidade){
    this.id = id;
    this.nome = nome;
    this.telefone = telefone;
    this.especialidade = especialidade;
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

  public String getEspecialidade(){
    return especialidade;
  }
  public void setEspecialidade(String especialidade){
    this.especialidade = especialidade;
  }

}