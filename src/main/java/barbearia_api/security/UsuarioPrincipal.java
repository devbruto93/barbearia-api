package barbearia_api.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Principal com a versão atual da conta para revogação imediata de JWT. */
public class UsuarioPrincipal extends User {
    private final long tokenVersion;

    public UsuarioPrincipal(String email, String senha,
            Collection<? extends GrantedAuthority> authorities, long tokenVersion) {
        super(email, senha, authorities);
        this.tokenVersion = tokenVersion;
    }

    public long getTokenVersion() { return tokenVersion; }
}
