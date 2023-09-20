package com.yunchi.core.user_system

import com.yunchi.Config
import com.yunchi.core.protocol.UserInfoResponse
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserExtraInfoTable
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.protocol.respondOK
import com.yunchi.core.utilities.DelegatedRouterBuilder
import com.yunchi.dirIfNotExist
import com.yunchi.fileIfNotExist
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.ktorm.dsl.*
import java.io.File

fun DelegatedRouterBuilder.configureInfo() {
    get("/userinfo") {
        val userId = call.parameters["userId"]
            .orEmpty().toLongOrNull() ?: return@get call.respondErr(
            "需要用户Id"
        )

        val user = Database
            .from(UserExtraInfoTable)
            .select(
                UserExtraInfoTable.name,
            )
            .where(UserExtraInfoTable.userId eq userId)
            .asIterable()
            .firstOrNull()
            ?: return@get call
                .respondErr("不存在此用户", HttpStatusCode.NotFound)

        val type = Database
            .from(UserIdentityTable)
            .select(UserIdentityTable.type)
            .where(
                UserIdentityTable.id eq userId
            )
            .asIterable()
            .firstOrNull()
            ?: return@get call
                .respondErr("不存在此用户", HttpStatusCode.NotFound)

        call.respondJson(
            UserInfoResponse(
                user[UserExtraInfoTable.name]!!,
                type[UserIdentityTable.type]!!.toString()
            )
        )
    }
    put("/user/icon", setOf("X-User-Id", "X-User-Code")) {
        val id = call.request.headers["X-User-Id"]
            .orEmpty().toLongOrNull()
            ?: return@put call.respondErr("X-User-Id格式错误")
        val code = call.request.headers["X-User-Code"]!!
        if (!checkCode(id, code))
            return@put call.respondErr("无效的用户登录")
        dirIfNotExist(Config.resource + "user/icon/")
        val f = fileIfNotExist("${Config.resource}user/icon/$id.png")
        f.outputStream().use {
            call.receiveStream().transferTo(it)
        }
        call.respondOK()
    }

    get("/user/icon") {
        val id = call.parameters["userId"]
            .orEmpty().toLongOrNull()
            ?: return@get call.respondErr("需要用户Id")
        val f = File("${Config.resource}user/icon/$id.png")
        if (f.exists())
            call.respondFile(f)
        else
            call.respond(HttpStatusCode.NotFound)
    }
}