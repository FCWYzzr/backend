package com.yunchi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*

fun Application.configurePing() {
    routing {
        get("/ping") {
            call.respond(HttpStatusCode.OK)
        }
        openAPI("/api", swaggerFile = "api.json")
    }
}
