package com.yunchi.core.utilities

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.netty.handler.codec.http.HttpMethod


private typealias Body = suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit
private typealias Headers = Set<String>

data class RouterRecord(
    val method: HttpMethod,
    val headers: Headers,
    val body: Body
)

class DelegatedRouterBuilder {
    private val records = mutableMapOf<String, MutableSet<RouterRecord>>()
    fun get(path: String, headers: Headers = emptySet(), body: Body) {
        records[path]?.add(RouterRecord(HttpMethod.GET, headers, body))
            ?: run {
                records[path] = mutableSetOf(RouterRecord(HttpMethod.GET, headers, body))
            }
    }

    fun post(path: String, headers: Headers = emptySet(), body: Body) {
        records[path]?.add(RouterRecord(HttpMethod.POST, headers, body))
            ?: run {
                records[path] = mutableSetOf(RouterRecord(HttpMethod.POST, headers, body))
            }
    }

    fun put(path: String, headers: Headers = emptySet(), body: Body) {
        records[path]?.add(RouterRecord(HttpMethod.PUT, headers, body))
            ?: run {
                records[path] = mutableSetOf(RouterRecord(HttpMethod.PUT, headers, body))
            }
    }

    fun delete(path: String, headers: Headers = emptySet(), body: Body) {
        records[path]?.add(RouterRecord(HttpMethod.DELETE, headers, body))
            ?: run {
                records[path] = mutableSetOf(RouterRecord(HttpMethod.DELETE, headers, body))
            }
    }

    fun bindTo(route: Route) {
        for ((path, methods) in records) {
            route.options(path) {
                val reqMethod = call.request.headers["Access-Control-Request-Method"]!!
                val headers = methods.firstOrNull {
                    it.method.name() == reqMethod
                }?.headers

                call.response.headers.append(
                    "Access-Control-Allow-Methods",
                    reqMethod
                )

                call.response.headers.append("Access-Control-Allow-Origin", "*")
                call.response.headers.append(
                    "Access-Control-Allow-Headers",
                    headers?.joinToString(", ", "Content-Type, ") ?: "*"
                )
                call.respond(HttpStatusCode.OK)
            }
            for ((method, _, body) in methods)
                when (method) {
                    HttpMethod.GET -> route.get(path) {
                        call.response.headers.append(
                            "Access-Control-Allow-Origin",
                            "*"
                        )
                        body.invoke(this, Unit)
                    }

                    HttpMethod.POST -> route.post(path) {
                        call.response.headers.append(
                            "Access-Control-Allow-Origin",
                            "*"
                        )
                        body.invoke(this, Unit)
                    }

                    HttpMethod.PUT -> route.put(path) {
                        call.response.headers.append(
                            "Access-Control-Allow-Origin",
                            "*"
                        )
                        body.invoke(this, Unit)
                    }

                    HttpMethod.DELETE -> route.delete(path) {
                        call.response.headers.append(
                            "Access-Control-Allow-Origin",
                            "*"
                        )
                        body.invoke(this, Unit)
                    }

                    else -> error("unsupported method")
                }
        }
    }
}

fun Route.buildCORSRoute(
    configure: DelegatedRouterBuilder.() -> Unit
) {
    val builder = DelegatedRouterBuilder()
    builder.configure()
    builder.bindTo(this)
}
