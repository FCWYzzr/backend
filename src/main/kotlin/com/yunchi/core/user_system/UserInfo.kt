package com.yunchi.core.user_system

import com.yunchi.core.protocol.UserInfoResponse
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserExtraInfoTable
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.protocol.orm.firstOrNull
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.utilities.DelegatedRouterBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import org.ktorm.dsl.*

fun DelegatedRouterBuilder.configureInfo() {
    get("/user/info") {
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

        val info = Database
            .from(UserIdentityTable)
            .select(UserIdentityTable.type)
            .where(
                UserIdentityTable.id eq userId
            )
            .firstOrNull()
            ?: return@get call
                .respondErr("不存在此用户", HttpStatusCode.NotFound)

        val img = Database
            .from(UserExtraInfoTable)
            .select(UserExtraInfoTable.image)
            .where(
                UserExtraInfoTable.userId eq userId
            ).firstOrNull()
            ?: return@get call
                .respondErr("内部错误", HttpStatusCode.InternalServerError)

        call.respondJson(
            UserInfoResponse(
                user[UserExtraInfoTable.name]!!,
                info[UserIdentityTable.type]!!.toString(),
                img[UserExtraInfoTable.image]!!
            )
        )
    }
}