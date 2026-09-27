package barbearia_api.service;

import barbearia_api.entity.Cliente;
import barbearia_api.exception.ResourceNotFoundException;
import barbearia_api.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
  private final ClienteRepository repository;

  public ClienteService(ClienteRepository repository){
    this.repository = repository;
  
  }

  public Cliente salvar(Cliente cliente){
    return repository.save(cliente);
  }

  public List<Cliente> listarTodos(){
    return repository.findAll();
  }

  public Cliente buscarPorId(Long id){
    return repository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Cliente", id));
  }

  public void deletar(Long id) {
    Cliente cliente = repository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Cliente", id));

    repository.delete(cliente);
  }

}
