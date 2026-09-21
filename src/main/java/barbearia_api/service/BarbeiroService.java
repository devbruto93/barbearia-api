package barbearia_api.service;

import barbearia_api.entity.Barbeiro;
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
            .orElseThrow(()-> new RuntimeException("Barbeiro não encontrado!"));
  }

  public void deletar(Long id) {
    repository.deleteById(id);
  }

}