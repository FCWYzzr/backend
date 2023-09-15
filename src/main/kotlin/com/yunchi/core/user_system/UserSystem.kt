package com.yunchi.core.user_system

import com.yunchi.configure
import com.yunchi.core.protocol.UserInfoResponse
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserExtraInfoTable
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.utilities.hashAutoSigninParted
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.*

fun Application.userSystem() {
    routing {
        get("/userinfo"){
            call.response.configure()
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

            call.respondJson(UserInfoResponse(
                user[UserExtraInfoTable.name]!!,
                type[UserIdentityTable.type]!!.toString()
            ))
        }

        configureSignIn()
        configureSignUp()
    }
}

fun checkCode(userId: Long, code: String): Boolean{
    val row = Database
        .from(UserIdentityTable)
        .select()
        .where(UserIdentityTable.id eq userId)
        .asIterable()
        .firstOrNull()
        ?: return false


    val lastTime = row[UserIdentityTable.lastSignin]!!
    println(row[UserIdentityTable.pwd])
    val fixedPart = hashAutoSigninParted(
        userId, row[UserIdentityTable.pwd]!!
    )
    return fixedPart(lastTime) == code
}


