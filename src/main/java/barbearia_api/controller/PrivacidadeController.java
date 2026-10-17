package barbearia_api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import barbearia_api.dto.AgendamentoResponseDTO;
import barbearia_api.dto.ExportacaoDadosDTO;
import barbearia_api.entity.Cliente;
import barbearia_api.service.AgendamentoService;
import barbearia_api.service.ClienteService;

@RestController
@RequestMapping("/privacidade")
public class PrivacidadeController {
    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;
    private final String contato;
    private final int retencaoDias;

    public PrivacidadeController(ClienteService clienteService,
            AgendamentoService agendamentoService,
            @Value("${privacy.contact-email:}") String contato,
            @Value("${privacy.retention.appointments-days:0}") int retencaoDias) {
        this.clienteService = clienteService;
        this.agendamentoService = agendamentoService;
        this.contato = contato;
        this.retencaoDias = retencaoDias;
    }

    @GetMapping("/aviso")
    public Map<String, Object> aviso() {
        return Map.of(
                "dadosTratados", List.of("email", "nome", "telefone", "horarios e status de agendamentos"),
                "finalidades", List.of("autenticacao", "cadastro e atendimento", "gestao de agendamentos"),
                "retencaoAgendamentosEncerradosDias", retencaoDias,
                "contatoPrivacidade", contato.isBlank() ? "Nao configurado" : contato,
                "direitos", List.of("acesso e exportacao", "correcao do cadastro", "exclusao da conta"),
                "observacao", "A barbearia deve informar a base legal aplicavel e os prazos de conservacao obrigatoria.");
    }

    @GetMapping("/me/exportacao")
    public ExportacaoDadosDTO exportar(@AuthenticationPrincipal UserDetails usuario) {
        Cliente cliente = clienteService.buscarPorEmailDoUsuario(usuario.getUsername());
        List<AgendamentoResponseDTO> agendamentos =
                agendamentoService.listarDoCliente(usuario.getUsername());
        return new ExportacaoDadosDTO(usuario.getUsername(), cliente.getNome(),
                cliente.getTelefone(), agendamentos);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirMinhaConta(@AuthenticationPrincipal UserDetails usuario) {
        clienteService.deletarPorEmailDoUsuario(usuario.getUsername());
        return ResponseEntity.noContent().build();
    }
}
