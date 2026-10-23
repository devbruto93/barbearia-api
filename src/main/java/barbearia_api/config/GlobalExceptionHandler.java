package barbearia_api.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import barbearia_api.exception.AcessoNegadoException;
import barbearia_api.exception.CredenciaisInvalidasException;
import barbearia_api.exception.ContaBloqueadaException;
import barbearia_api.exception.EmailJaCadastradoException;
import barbearia_api.exception.HorarioIndisponivelException;
import barbearia_api.exception.OperacaoInvalidaException;
import barbearia_api.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduz excecoes em respostas HTTP com corpo JSON consistente.
 *
 * Sem isso, qualquer RuntimeException vira 500 com a pagina de erro do Tomcat
 * e o cliente recebe HTML onde esperava JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> credenciaisInvalidas(
            CredenciaisInvalidasException e, HttpServletRequest request) {
        return resposta(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage(), request);
    }

    @ExceptionHandler(ContaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> contaBloqueada(
            ContaBloqueadaException e, HttpServletRequest request) {
        return resposta(HttpStatus.LOCKED, "Account Locked", e.getMessage(), request);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, Object>> emailJaCadastrado(
            EmailJaCadastradoException e, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "Conflict", e.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(
            ResourceNotFoundException e, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Not Found", e.getMessage(), request);
    }

    /**
     * Usuario autenticado mexendo em recurso de outro. 403 e nao 404: o
     * recurso existe, o que falta e permissao.
     */
    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(
            AcessoNegadoException e, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage(), request);
    }

    /**
     * Horario ja reservado para o mesmo barbeiro. 409 Conflict expressa a
     * disputa pelo recurso melhor do que um 400 de validacao.
     */
    @ExceptionHandler(HorarioIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> horarioIndisponivel(
            HorarioIndisponivelException e, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "Conflict", e.getMessage(), request);
    }

    @ExceptionHandler(OperacaoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> operacaoInvalida(
            OperacaoInvalidaException e, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "Conflict", e.getMessage(), request);
    }

    /**
     * Violacao de unicidade chega aqui quando o controller tenta gravar, por
     * exemplo, um telefone que ja existe. Traduzir para 409 evita o 500 que o
     * cliente receberia sem tratamento nenhum.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> violacaoDeIntegridade(
            DataIntegrityViolationException e, HttpServletRequest request) {
        if (sqlState(e, "23P01")) {
            return resposta(HttpStatus.CONFLICT, "Conflict",
                    "Este horario acabou de ser ocupado. Atualize a disponibilidade e tente outro horario.",
                    request);
        }
        return resposta(HttpStatus.CONFLICT, "Conflict",
                "Ja existe um registro com esse dado.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> conflitoDeEdicaoConcorrente(
            OptimisticLockingFailureException e, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, "Conflict",
                "Este agendamento foi alterado por outra operacao. Atualize os dados e tente novamente.",
                request);
    }

    private boolean sqlState(Throwable erro, String esperado) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof java.sql.SQLException sql
                    && esperado.equals(sql.getSQLState())) return true;
            atual = atual.getCause();
        }
        return false;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }

        Map<String, Object> body = corpo(HttpStatus.BAD_REQUEST, "Bad Request",
                "Dados invalidos.", request);
        body.put("campos", campos);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNaoPermitido(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                "Metodo HTTP nao permitido para este recurso.", request);
    }

    /**
     * Rede de seguranca para qualquer excecao nao mapeada. O log recebe a
     * excecao completa para diagnostico, enquanto a resposta HTTP devolve
     * apenas uma mensagem generica: stack trace e nomes de tabela em resposta
     * de erro sao vazamento de informacao.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInesperado(
            Exception e, HttpServletRequest request) {

        log.error("Erro nao tratado em {} {}", request.getMethod(), request.getRequestURI(), e);

        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Erro interno inesperado.", request);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String error,
            String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(corpo(status, error, message, request));
    }

    private Map<String, Object> corpo(HttpStatus status, String error,
            String message, HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", Instant.now().toString());

        return body;
    }
}
