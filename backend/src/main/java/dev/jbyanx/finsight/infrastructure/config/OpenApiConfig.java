package dev.jbyanx.finsight.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI (Swagger UI) con soporte de JWT.
 *
 * El SecurityScheme "bearerAuth" es lo que habilita el CANDADITO en Swagger UI:
 * se pega el access token una vez y todas las peticiones lo mandan en el header
 * Authorization. Sin esto, probar los endpoints protegidos desde el navegador
 * obliga a copiar el header a mano en cada llamada.
 *
 * UI:    http://localhost:8080/swagger-ui.html
 * JSON:  http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI finsightOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finsight API")
                        .version("v1")
                        .description("""
                                Gestor de finanzas personales con recomendaciones por IA.

                                Autenticación: la mayoría de endpoints requieren un access token JWT.
                                Obtén uno iniciando sesión (OAuth2 Google) y pégalo en el botón
                                Authorize (candado). No incluyas el prefijo "Bearer": Swagger lo agrega.
                                """)
                        .contact(new Contact().name("Jbyanx")))
                // Aplica el esquema a todos los endpoints por defecto.
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token JWT (válido 15 minutos).")));
    }
}