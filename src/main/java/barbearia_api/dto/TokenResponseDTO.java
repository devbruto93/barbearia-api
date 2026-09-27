package barbearia_api.dto;

import java.time.Instant;

public record TokenResponseDTO(
    String token,
    String email,
    String role,
    Instant expiraEm
) {
}
