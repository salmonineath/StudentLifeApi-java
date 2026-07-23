package com.studentlife.studentlifejava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    // Must match CookieUtil.ACCESS_TOKEN_COOKIE - auth rides on an httpOnly
    // cookie set by /api/v1/auth/login, not on an Authorization header.
    private static final String COOKIE_AUTH_SCHEME = "cookieAuth";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";

    @Bean
    public OpenAPI studentLifeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StudentLife API")
                        .version("v1")
                        .description("""
                                REST API for the StudentLife application.

                                **Authentication** is cookie-based JWT:
                                1. Call `POST /api/v1/auth/login` (or `/register`) with valid credentials \
                                using *Try it out*.
                                2. The server sets httpOnly `accessToken` and `refreshToken` cookies - \
                                the browser attaches them automatically to every subsequent request, \
                                so all *Try it out* calls are authenticated from then on.
                                3. Use `POST /api/v1/auth/refresh` when the access token expires, \
                                and `POST /api/v1/auth/logout` to clear the session.

                                Because the cookies are httpOnly, the *Authorize* dialog cannot set them \
                                manually - logging in through the endpoint above is the intended flow.
                                Admin-only endpoints (`/api/v1/admin/**`) additionally require the \
                                `admin` role."""))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(ACCESS_TOKEN_COOKIE)
                                .description("JWT access token issued as an httpOnly cookie by "
                                        + "POST /api/v1/auth/login. Sent automatically by the browser.")))
                // Applied globally; the customizer below strips it from the
                // endpoints SecurityConfig declares permitAll.
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH_SCHEME));
    }

    // Mirrors the permitAll matchers in SecurityConfig so public endpoints do
    // not display a padlock. Kept here (instead of annotating controllers) so
    // Swagger concerns stay out of the controller code.
    @Bean
    public OpenApiCustomizer publicEndpointsCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.startsWith("/api/v1/auth") || path.equals("/health")) {
                pathItem.readOperations()
                        .forEach(operation -> operation.setSecurity(Collections.emptyList()));
            }
        });
    }

    // Pins "Authentication" first in Swagger UI, then the rest alphabetically.
    // Must run with tags-sorter NOT set to "alpha" in application.yaml - that's
    // a client-side re-sort that would undo this ordering on every page load.
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            Map<String, String> descriptionsByTag = new LinkedHashMap<>();
            if (openApi.getTags() != null) {
                openApi.getTags().forEach(tag -> descriptionsByTag.put(tag.getName(), tag.getDescription()));
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getTags() != null) {
                            operation.getTags().forEach(name -> descriptionsByTag.putIfAbsent(name, null));
                        }
                    }));

            openApi.setTags(descriptionsByTag.entrySet().stream()
                    .map(e -> new Tag().name(e.getKey()).description(e.getValue()))
                    .sorted(Comparator
                            .comparing((Tag t) -> !"Authentication".equals(t.getName()))
                            .thenComparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList());
        };
    }
}
