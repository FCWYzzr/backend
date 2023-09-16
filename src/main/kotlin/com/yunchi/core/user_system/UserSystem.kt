package com.yunchi.core.user_system

import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.utilities.buildCORSRoute
import com.yunchi.core.utilities.hashAutoSigninParted
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.ktorm.dsl.*

fun Application.userSystem() {
    routing {
        buildCORSRoute {
            configureSignIn()
            configureSignUp()
            configureInfo()
        }
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


