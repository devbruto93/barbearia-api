package barbearia_api.exception;

public class ContaBloqueadaException extends RuntimeException {
    public ContaBloqueadaException() {
        super("Conta temporariamente bloqueada apos 3 tentativas. Aguarde o prazo ou contate o suporte.");
    }
}
