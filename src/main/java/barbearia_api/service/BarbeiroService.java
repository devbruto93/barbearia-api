package barbearia_api.service;

import barbearia_api.entity.Barbeiro;
import barbearia_api.exception.ResourceNotFoundException;
import barbearia_api.repository.BarbeiroRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarbeiroService {
  private final BarbeiroRepository repository;

  public BarbeiroService(BarbeiroRepository repository){
    this.repository = repository;
  }

  public Barbeiro salvar(Barbeiro barbeiro){
    return repository.save(barbeiro);
  }

  public List<Barbeiro> listarTodos(){
    return repository.findAll();
  }

  public Barbeiro buscarPorId(Long id){
    return repository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Barbeiro", id));
  }

  public Barbeiro atualizar(Long id, String nome, String telefone, String especialidade) {
    Barbeiro barbeiro = buscarPorId(id);
    barbeiro.setNome(nome);
    barbeiro.setTelefone(telefone);
    barbeiro.setEspecialidade(especialidade);
    return repository.save(barbeiro);
  }

  public void deletar(Long id) {
    Barbeiro barbeiro = repository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Barbeiro", id));

    repository.delete(barbeiro);
  }

}
