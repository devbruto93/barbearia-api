package barbearia_api.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Ja existe um usuario cadastrado com este email.");
    }
}
