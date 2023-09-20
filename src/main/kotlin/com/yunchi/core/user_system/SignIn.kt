package com.yunchi.core.user_system

import com.yunchi.Project.emailTemplate
import com.yunchi.Project.numberTemplate
import com.yunchi.Project.phoneTemplate
import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.UserExtraInfoTable
import com.yunchi.core.protocol.orm.UserIdentityTable
import com.yunchi.core.protocol.orm.firstOrNull
import com.yunchi.core.utilities.DelegatedRouterBuilder
import com.yunchi.core.utilities.hashAutoSignin
import com.yunchi.core.utilities.hashAutoSigninParted
import io.ktor.http.*
import io.ktor.server.application.*
import org.ktorm.dsl.*
import java.time.Instant

fun DelegatedRouterBuilder.configureSignIn() {
    post("/signin"){
        val info = call.receiveJson<SignInArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        val userId = Database
            .from(UserExtraInfoTable)
            .select(UserExtraInfoTable.userId)
            .where {
                val selectors = mutableListOf(
                    UserExtraInfoTable.name eq info.identifier
                )

                if (info.identifier
                        .matches(numberTemplate)) {
                    val longV = info.identifier.toLong()
                    selectors.add(
                        UserExtraInfoTable.userId
                            eq longV
                    )
                    if (info.identifier
                            .matches(phoneTemplate))
                        selectors.add(
                            UserExtraInfoTable.phone
                                eq longV
                        )
                }
                else if (info.identifier
                    .matches(emailTemplate))
                    selectors.add(
                        UserExtraInfoTable.email
                            eq info.identifier
                    )

                selectors.reduce { v1, v2 ->
                    v1 or v2
                }
            }
            .asIterable()
            .firstNotNullOfOrNull { it[UserExtraInfoTable.userId] }
            ?: return@post call.respondErr(
                "不存在该用户", HttpStatusCode.NotFound
            )

        Database
            .from(UserIdentityTable)
            .select()
            .where(
                (UserIdentityTable.id eq userId) and
                    (UserIdentityTable.pwd eq info.password)
            ).firstOrNull()
            ?: return@post call.respondErr("密码错误")

        val timestamp = Instant.now()

        Database
            .update(UserIdentityTable){
                set(it.lastSignin, timestamp)
                where {
                    it.id eq userId
                }
            }

        return@post call.respondJson(SigninResponse(
            userId,
            hashAutoSignin(
                userId, info.password, timestamp
            )
        ))
    }

    post("/auto-signin"){
        val param = call.receiveJson<AutoSignInArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        val row = Database
            .from(UserIdentityTable)
            .select()
            .where{
                UserIdentityTable.id eq param.userId
            }
            .firstOrNull()
            ?: return@post call.respondErr(
                "不存在该用户", HttpStatusCode.NotFound
            )


        val lastTime = row[UserIdentityTable.lastSignin]!!
        println(row[UserIdentityTable.pwd])
        val fixedPart = hashAutoSigninParted(
            param.userId, row[UserIdentityTable.pwd]!!
        )
        if (fixedPart(lastTime) != param.code)
            return@post call.respondErr(
                "自动登录失败"
            )

        val current = Instant.now()
        val newCode = fixedPart(current)

        Database
            .update(UserIdentityTable){
                set(it.lastSignin, current)
                where { it.id eq param.userId }
            }

        call.respondJson(CodeResponse(newCode))
    }
}

