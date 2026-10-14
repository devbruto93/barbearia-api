package barbearia_api.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Horarios semanais de atendimento, configurados por dia da semana. */
@Component
@ConfigurationProperties(prefix = "api.agendamento.expediente")
public class ExpedienteConfig {

    /** Formato de cada valor: HH:mm-HH:mm; dias ausentes ficam fechados. */
    private Map<DayOfWeek, String> horarios = new EnumMap<>(DayOfWeek.class);

    public Map<DayOfWeek, String> getHorarios() {
        return horarios;
    }

    public void setHorarios(Map<DayOfWeek, String> horarios) {
        this.horarios = horarios == null ? new EnumMap<>(DayOfWeek.class) : horarios;
    }

    public boolean permite(java.time.LocalDateTime inicio, long duracaoMinutos) {
        String faixa = horarios.get(inicio.getDayOfWeek());
        if (faixa == null || faixa.isBlank()) {
            return false;
        }

        String[] limites = faixa.split("-", -1);
        if (limites.length != 2) {
            throw new IllegalStateException("Expediente invalido: use o formato HH:mm-HH:mm.");
        }

        LocalTime abertura = LocalTime.parse(limites[0]);
        LocalTime fechamento = LocalTime.parse(limites[1]);
        java.time.LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);

        return !inicio.toLocalTime().isBefore(abertura)
                && !fim.toLocalTime().isAfter(fechamento)
                && inicio.toLocalDate().equals(fim.toLocalDate());
    }
}
