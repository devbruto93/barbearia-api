package barbearia_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import barbearia_api.security.JwtAuthenticationFilter;
import barbearia_api.security.AuthRateLimitFilter;

import tools.jackson.databind.ObjectMapper;

/**
 * Define a cadeia de filtros de seguranca da API.
 *
 * Enquanto nenhum bean {@link SecurityFilterChain} existir, o Spring Boot aplica
 * uma configuracao padrao que protege TODOS os endpoints com HTTP Basic e senha
 * gerada no console. Esse e o oposto do que uma API stateless com JWT precisa.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/error",
        "/auth/login",
        "/auth/cadastro",
        "/privacidade/aviso",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthRateLimitFilter authRateLimitFilter) throws Exception {

        http
            .cors(cors -> { })
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper)))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/admin/desbloqueios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                .requestMatchers("/privacidade/me/**").hasRole("CLIENTE")
                // A primeira regra que casa ganha. /clientes/me precisa vir
                // antes de /clientes/**, senao o cliente cairia na regra de admin.
                .requestMatchers("/clientes/me").hasRole("CLIENTE")
                .requestMatchers("/clientes/**").hasRole("ADMIN")
                .requestMatchers("/barbeiros/**").hasRole("ADMIN")
                // A agenda tem donos diferentes por operacao: o cliente cria e
                // enxerga a propria; o admin ve tudo. /agendamentos/me precisa
                // vir antes do curinga, senao cai na regra de admin.
                .requestMatchers(HttpMethod.GET, "/agendamentos/me").hasRole("CLIENTE")
                .requestMatchers(HttpMethod.POST, "/agendamentos").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/cancelar").authenticated()
                .requestMatchers(HttpMethod.PUT, "/agendamentos/*").hasRole("ADMIN")
                // Enumerar operações administrativas por método torna o controle
                // independente de mudanças no padrão de correspondência de rotas.
                .requestMatchers(HttpMethod.GET, "/agendamentos", "/agendamentos/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/agendamentos/*/concluir").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(authRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * O fluxo de login fica explicito aqui: o DaoAuthenticationProvider busca o
     * usuario pelo email e compara a senha com o BCrypt. O ProviderManager e so
     * o delegador que chama o provider e traduz o resultado em-sucesso ou excecao.
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }
}
