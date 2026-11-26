package gaselvolcan.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger en http://localhost:8081/swagger-ui.html, con boton Authorize para pegar el JWT. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI definicion() {
        return new OpenAPI()
            .info(new Info().title("Gas El Volcan - API Auth").version("v1")
                .description("Usuarios, login, registro y emision del JWT que valida la API de Pedidos. EA3 DSY1104."))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
