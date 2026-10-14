package barbearia_api.exception;

/**
 * O usuario autenticado tentou agir sobre um recurso que pertence a outro.
 * Vira 403, e nao 404, porque o recurso existe e o problema e de permissao.
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String message) {
        super(message);
    }
}
