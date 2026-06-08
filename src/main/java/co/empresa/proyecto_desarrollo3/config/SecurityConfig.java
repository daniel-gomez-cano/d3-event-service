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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ── Endpoints del organizador ────────────────────────
                        .requestMatchers(HttpMethod.GET,   "/api/v1/events/my-events").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.POST,  "/api/v1/events").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/events/*/publish").hasRole("ORGANIZER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/events/*/cancel").hasRole("ORGANIZER")

                        // ── Endpoints internos (order-service) ───────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/reserve").hasRole("ORDER_SERVICE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/release").hasRole("ORDER_SERVICE")

                        // ── Endpoints internos (comunicación entre microservicios) ────────
                        .requestMatchers(HttpMethod.GET, "/api/internal/**").permitAll()

                        // ── Catálogo público ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()

                        // ── Health check ──────────────────────────────────────
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Admin ─────────────────────────────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                // Usar oauth2ResourceServer para que @AuthenticationPrincipal Jwt
                // siga funcionando. El JwtDecoder real se define abajo.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(gatewayJwtConverter()))
                );

        return http.build();
    }

    /**
     * JwtDecoder de paso que confía en el token ya validado por el Gateway.
     *
     * En producción: el Gateway ya verificó el JWT contra Keycloak y lo
     * reenvía al event-service junto con los headers X-User-*. Este decoder
     * simplemente reconstruye el objeto Jwt para @AuthenticationPrincipal.
     *
     * En tests: spring-security-test intercepta con jwt() y NUNCA invoca
     * este bean, por lo que los tests de integración siguen funcionando
     * exactamente igual que antes.
     *
     * Alternativa si quieres doble validación (Gateway + event-service):
     * elimina este @Bean y usa en application.properties:
     *   spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${KEYCLOAK_JWK_SET_URI}
     */


    /**
     * Converter que extrae roles desde el token ya enriquecido por el Gateway.
     * Compatible con tokens Keycloak directos (realm_access.roles) y con
     * el claim "role" que el gateway puede inyectar.
     */
    @Bean
    public JwtAuthenticationConverter gatewayJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            // Opción A: token Keycloak completo (realm_access.roles)
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(r -> new SimpleGrantedAuthority(
                                r.startsWith("ROLE_") ? r : "ROLE_" + r))
                        .collect(Collectors.toList());
            }

            // Opción B: claim "role" inyectado por el gateway (X-User-Role)
            String roleClaim = jwt.getClaim("role");
            if (roleClaim != null && !roleClaim.isBlank()) {
                return List.of(new SimpleGrantedAuthority(
                        roleClaim.startsWith("ROLE_") ? roleClaim : "ROLE_" + roleClaim));
            }

            return List.of();
        });
        return converter;
    }
}