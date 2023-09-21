package com.yunchi.core.utility_system

import com.yunchi.Config
import com.yunchi.core.protocol.UrlResponse
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.protocol.respondOK
import com.yunchi.core.user_system.checkCode
import com.yunchi.core.utilities.buildCORSRoute
import com.yunchi.core.utilities.genSnowflake
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File


fun Application.utilitySystem() {
    routing {
        buildCORSRoute {
            put("/image", setOf("X-User-Id", "X-User-Code")) {
                val userId = call.request.headers["X-User-Id"].orEmpty().toLongOrNull()
                    ?: return@put call.respondErr("用户Id格式错误")
                if (!checkCode(
                        userId,
                        call.request.headers["X-User-Code"]!!
                    )
                )
                    return@put call.respondErr("非法操作")

                val multipart = call.receiveMultipart()

                multipart.forEachPart {
                    if (it !is PartData.FileItem)
                        return@forEachPart

                    val imageId = genSnowflake("image")

                    val file = File(
                        Config.dirs.repo,
                        "$imageId.$userId.picture"
                    )

                    it.streamProvider()
                        .use { input ->
                            file.outputStream()
                                .buffered()
                                .use(input::transferTo)
                        }

                    call.respondJson(
                        UrlResponse(
                            "/image?id=$imageId?userId=$userId"
                        )
                    )
                }
            }

            get("/image") {
                val imageId = call.parameters["id"]
                    .orEmpty().toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val userId = call.parameters["userId"]
                    .orEmpty().toLongOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)

                val file = File(Config.dirs.repo, "$imageId.$userId.picture")
                if (!file.exists())
                    return@get call.respond(HttpStatusCode.NotFound)

                call.respondOutputStream(ContentType.parse("image/*")) {
                    file.inputStream().use {
                        it.transferTo(this)
                    }
                }
            }

            delete("/image", setOf("X-User-Id", "X-User-Code")) {
                val userId = call.request.headers["X-User-Id"].orEmpty().toLongOrNull()
                    ?: return@delete call.respondErr("用户Id格式错误")
                if (!checkCode(
                        userId,
                        call.request.headers["X-User-Code"]!!
                    )
                )
                    return@delete call.respondErr("非法操作")

                val imageId = call.parameters["id"]
                    .orEmpty().toLongOrNull()
                    ?: return@delete call.respondErr("请求参数格式错误")

                val file = File(Config.dirs.repo, "$imageId.$userId.picture")
                if (file.exists())
                    file.delete()

                call.respondOK()
            }
        }
    }
}