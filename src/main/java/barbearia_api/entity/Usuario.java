package barbearia_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity 
@Table (name = "usuarios")
public class Usuario {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true)
    private String email;

    @Column (nullable =  false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "tentativas_login_falhas", nullable = false)
    private int tentativasLoginFalhas;

    @Column(name = "login_bloqueado", nullable = false)
    private boolean loginBloqueado;

    @Column(name = "login_bloqueado_ate")
    private Instant loginBloqueadoAte;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getTentativasLoginFalhas() { return tentativasLoginFalhas; }
    public void setTentativasLoginFalhas(int tentativasLoginFalhas) { this.tentativasLoginFalhas = tentativasLoginFalhas; }
    public boolean isLoginBloqueado() { return loginBloqueado; }
    public void setLoginBloqueado(boolean loginBloqueado) { this.loginBloqueado = loginBloqueado; }
    public Instant getLoginBloqueadoAte() { return loginBloqueadoAte; }
    public void setLoginBloqueadoAte(Instant loginBloqueadoAte) { this.loginBloqueadoAte = loginBloqueadoAte; }
    public long getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(long tokenVersion) { this.tokenVersion = tokenVersion; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    
}
