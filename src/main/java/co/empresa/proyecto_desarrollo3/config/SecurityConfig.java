package co.empresa.proyecto_desarrollo3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Cadena de filtros de seguridad.
     * Define qué endpoints son públicos y cuáles requieren autenticación.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Sin estado: cada request lleva su JWT, no hay sesión en servidor
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Deshabilitar CSRF: no aplica en APIs REST stateless con JWT
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ── Endpoints públicos ───────────────────────────────
                        // Catálogo de eventos: cualquiera puede ver
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/{id}").permitAll()

                        // Health check para Docker y monitoreo
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Endpoints del organizador ────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/events").hasRole("EVENT_CREATOR")
                        .requestMatchers(HttpMethod.GET,  "/api/v1/events/my-events").hasRole("EVENT_CREATOR")
                        .requestMatchers(HttpMethod.PATCH,"/api/v1/events/{id}/cancel").hasRole("EVENT_CREATOR")

                        // ── Endpoints de administrador ───────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ── Cualquier otra ruta requiere autenticación ───────
                        .anyRequest().authenticated()
                )

                // Configurar como Resource Server que valida JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    /**
     * Converter que extrae los roles desde el claim de Keycloak.
     *
     * Keycloak pone los roles en una estructura anidada:
     *   "realm_access": { "roles": ["ROLE_ADMIN", "ROLE_EVENT_CREATOR"] }
     *
     * Spring Security espera GrantedAuthority con prefijo "ROLE_".
     * Este converter hace la traducción automáticamente.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Leer el claim realm_access del token de Keycloak
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            // Convertir cada rol a SimpleGrantedAuthority
            // Keycloak ya incluye el prefijo ROLE_ si así se configuró el mapper.
            // Si no, se agrega aquí: "ROLE_" + role
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role
                    ))
                    .collect(Collectors.toList());
        });

        return converter;
    }
}

