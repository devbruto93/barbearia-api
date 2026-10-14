package barbearia_api.exception;

/**
 * Operacao invalida para o estado atual do recurso, como cancelar um
 * agendamento ja concluido. Distinta de erro de validacao do payload: o dado
 * esta correto, mas a transicao de estado nao e permitida.
 */
public class OperacaoInvalidaException extends RuntimeException {

    public OperacaoInvalidaException(String message) {
        super(message);
    }
}
