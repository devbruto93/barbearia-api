package barbearia_api.dto;

import barbearia_api.entity.Barbeiro;

/** Campos do barbeiro que a API disponibiliza. */
public record BarbeiroResponseDTO(Long id, String nome, String telefone, String especialidade) {

    public static BarbeiroResponseDTO de(Barbeiro barbeiro) {
        return new BarbeiroResponseDTO(barbeiro.getId(), barbeiro.getNome(),
                barbeiro.getTelefone(), barbeiro.getEspecialidade());
    }
}
