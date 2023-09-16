package com.yunchi.core.user_system

import com.yunchi.core.protocol.UserInfoResponse
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserExtraInfoTable
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.utilities.DelegatedRouterBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.ktorm.dsl.*

fun DelegatedRouterBuilder.configureInfo() {
    get("/userinfo") {
        val userId = call.parameters["userId"]
            .orEmpty().toLongOrNull() ?: return@get call.respond(
            HttpStatusCode.BadRequest
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
                .respondErr("no such user", HttpStatusCode.NotFound)

        val type = Database
            .from(UserIdentityTable)
            .select(UserIdentityTable.type)
            .where(
                UserIdentityTable.id eq userId
            )
            .asIterable()
            .firstOrNull()
            ?: return@get call
                .respondErr("no such user", HttpStatusCode.NotFound)

        call.respondJson(
            UserInfoResponse(
                user[UserExtraInfoTable.name]!!,
                type[UserIdentityTable.type]!!.toString()
            )
        )
    }
}