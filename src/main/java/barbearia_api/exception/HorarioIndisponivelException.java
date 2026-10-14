package barbearia_api.exception;

public class HorarioIndisponivelException extends RuntimeException {

    public HorarioIndisponivelException() {
        super("O barbeiro ja possui um agendamento neste horario.");
    }

    public HorarioIndisponivelException(String message) {
        super(message);
    }
}
