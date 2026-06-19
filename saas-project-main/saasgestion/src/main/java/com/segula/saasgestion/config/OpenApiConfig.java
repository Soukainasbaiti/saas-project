package com.segula.saasgestion.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "SaaS Gestion API",
        version = "1.0",
        description = "API REST pour la gestion de projets d'ingénierie — suivi budgétaire, ressources, risques, WIP et indicateurs.",
        contact = @Contact(name = "Segula Technologies", email = "contact@segula.eu")
    ),
    servers = {
        @Server(url = "/api", description = "Serveur courant"),
        @Server(url = "https://saas-gestion-production.up.railway.app/api", description = "Production Railway")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Entrez le token JWT obtenu via POST /auth/login"
)
public class OpenApiConfig {
}
