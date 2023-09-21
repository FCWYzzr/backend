package com.yunchi.plugins

import com.yunchi.Config
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.nio.file.Files
import java.nio.file.Path

fun Application.configurePing() {
    routing {
        get("/ping") {
            call.respond(HttpStatusCode.OK)
        }
        openAPI("/api", swaggerFile = Config.dirs.resource + "API.json")
        get("/api-group"){
            call.respondText(Files.readString(
                Path.of(
                    Config.dirs.resource + "api-group.html"
                )
            ), ContentType.parse("text/html"))
        }
    }
}
