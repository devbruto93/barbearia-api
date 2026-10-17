package barbearia_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

import barbearia_api.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.tentativasLoginFalhas = u.tentativasLoginFalhas + 1 " +
            "where u.email = :email and u.loginBloqueado = false")
    int registrarFalhaLogin(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.loginBloqueado = true, u.loginBloqueadoAte = :ate, " +
            "u.tokenVersion = u.tokenVersion + 1 where u.email = :email " +
            "and u.tentativasLoginFalhas >= 3 and u.loginBloqueado = false")
    int bloquearAposTerceiraFalha(@Param("email") String email, @Param("ate") Instant ate);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.tentativasLoginFalhas = 0 where u.email = :email " +
            "and u.loginBloqueado = false")
    int limparFalhasLogin(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.tentativasLoginFalhas = 0, u.loginBloqueado = false, " +
            "u.loginBloqueadoAte = null, u.tokenVersion = u.tokenVersion + 1 where u.email = :email")
    int desbloquearLogin(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.tentativasLoginFalhas = 0, u.loginBloqueado = false, " +
            "u.loginBloqueadoAte = null where u.email = :email and u.loginBloqueado = true " +
            "and u.loginBloqueadoAte <= :agora")
    int liberarBloqueioExpirado(@Param("email") String email, @Param("agora") Instant agora);

    @Modifying
    @Transactional
    @Query("update Usuario u set u.tokenVersion = u.tokenVersion + 1 where u.email = :email")
    int revogarTokens(@Param("email") String email);
}
