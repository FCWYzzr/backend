package com.yunchi.core.user_system

import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.utilities.*
import io.ktor.server.application.*
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import java.time.Instant

fun DelegatedRouterBuilder.configureSignUp() {
    post("/verify"){
        val (username, contact, type) = call.receiveJson<VerifyRequestArgument>()
            ?: return@post call.respondErr("参数格式错误")

        val code = VerifyCode.newCode(contact, type)
            ?: return@post call.respondErr("该联系方式已被注册")

        sendVerifyCode(username, contact, code, type)
        call.respondOK()
    }

    post("/signup"){
        val info = call.receiveJson<SignUpArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        val type = VerifyCode.matchCode(info.contact, info.code)
            ?: return@post call.respondErr("验证码不正确")

        val id = genSnowflake("user")
        val current = Instant.now()

        Database
            .insert(UserIdentityTable){
                set(UserIdentityTable.id, id)
                set(UserIdentityTable.type, UserType.UNKNOWN)
                set(UserIdentityTable.pwd, info.password)
                set(UserIdentityTable.lastSignin, current)
            }

        Database
            .insert(UserExtraInfoTable){
                set(UserExtraInfoTable.userId, id)
                set(UserExtraInfoTable.name, info.username)
                if (type == VerifyType.PHONE)
                    set(UserExtraInfoTable.phone, info.contact.toLong())
                else
                    set(UserExtraInfoTable.email, info.contact)
            }

        Database
            .delete(CandidateVerifyCodeTable){
                it.userContact eq info.contact
            }

        call.respondJson(SigninResponse(
            id, hashAutoSignin(id, info.password, current)
        ))
    }
}
