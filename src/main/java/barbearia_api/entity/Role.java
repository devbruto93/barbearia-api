package barbearia_api.entity;

/**
 * Nivel de acesso do usuario.
 *
 * CLIENTE: acessa apenas o proprio cadastro.
 * ADMIN: gerencia os barbeiros e os clientes.
 */
public enum Role {
    CLIENTE,
    ADMIN
}
