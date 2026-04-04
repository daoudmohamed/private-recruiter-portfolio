package com.knowledgebase.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Forwards non-API browser routes to the React entrypoint so the SPA can be
 * served by the same Spring Boot runtime as the backend APIs.
 */
@Controller
class SpaController {

    @GetMapping(
        "/",
        "/{path:^(?!api|actuator|swagger-ui|api-docs|webjars)[^.]*}",
        "/**/{path:^(?!api|actuator|swagger-ui|api-docs|webjars)[^.]*}"
    )
    fun index(): String = "forward:/index.html"
}
