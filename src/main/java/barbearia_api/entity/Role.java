package barbearia_api.entity;

/**
 * Nivel de acesso do usuario.
 *
 * CLIENTE: acessa apenas os proprios dados.
 * ADMIN: gerencia a agenda, os barbeiros e os clientes.
 */
public enum Role {
    CLIENTE,
    ADMIN
}
