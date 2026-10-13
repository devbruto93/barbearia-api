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

  public Cliente buscarPorEmailDoUsuario(String email){
    return repository.findByUsuarioEmail(email)
            .orElseThrow(()-> new ResourceNotFoundException(
                    "Cliente nao encontrado para o usuario autenticado."));
  }

  public Cliente atualizar(Long id, String nome, String telefone){
    return aplicar(buscarPorId(id), nome, telefone);
  }

  public Cliente atualizarPorEmailDoUsuario(String email, String nome, String telefone){
    return aplicar(buscarPorEmailDoUsuario(email), nome, telefone);
  }

  private Cliente aplicar(Cliente cliente, String nome, String telefone){
    cliente.setNome(nome);
    cliente.setTelefone(telefone);
    return repository.save(cliente);
  }

  public void deletar(Long id) {
    Cliente cliente = repository.findById(id)
            .orElseThrow(()-> new ResourceNotFoundException("Cliente", id));

    repository.delete(cliente);
  }

}
