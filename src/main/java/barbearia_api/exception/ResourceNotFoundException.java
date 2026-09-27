package barbearia_api.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Long id) {
        super(recurso + " nao encontrado: id " + id);
    }
}
