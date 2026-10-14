package barbearia_api.dto;

import barbearia_api.entity.Cliente;

/** Campos publicáveis do perfil, sem associação à conta de autenticação. */
public record ClienteResponseDTO(Long id, String nome, String telefone) {

    public static ClienteResponseDTO de(Cliente cliente) {
        return new ClienteResponseDTO(cliente.getId(), cliente.getNome(), cliente.getTelefone());
    }
}
