package com.yunchi.core.user_system

import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.utilities.*
import io.ktor.server.application.*
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import java.time.Instant

fun DelegatedRouterBuilder.configureSignUp() {
    post("/visitor") {
        val visitor = call.receiveJson<VisitorSignInArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        val id = genSnowflake("user")
        val current = Instant.now()

        Database
            .insert(UserIdentityTable) {
                set(UserIdentityTable.id, id)
                set(UserIdentityTable.type, UserType.VISITOR)
                set(UserIdentityTable.pwd, "")
                set(UserIdentityTable.lastSignin, current)
            }

        Database
            .insert(UserExtraInfoTable) {
                set(UserExtraInfoTable.userId, id)
                set(UserExtraInfoTable.name, visitor.name)
            }

        call.respondJson(
            SigninResponse(
                id, hashAutoSignin(id, "", current)
            )
        )
    }

    post("/visitor/upgrade", setOf("X-User-Id", "X-User-Code")) {
        val info = call.receiveJson<VisitorUpgradeArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        val userId = call.request.headers["X-User-Id"]!!
            .toLongOrNull()
            ?: return@post call.respondErr("用户Id格式错误")

        val code = call.request.headers["X-User-Code"]!!

        if (!checkCode(userId, code))
            return@post call.respondErr("用户验证失败")

        VerifyCode.matchCode(info.contact, info.code)
            ?: return@post call.respondErr("验证码不正确")

        val now = Instant.now()

        Database
            .update(UserIdentityTable) {
                set(UserIdentityTable.type, UserType.UNKNOWN)
                set(UserIdentityTable.pwd, info.password)
                set(UserIdentityTable.lastSignin, now)
                where {
                    UserIdentityTable.id eq userId
                }
            }

        Database
            .update(UserExtraInfoTable) {
                set(UserExtraInfoTable.name, info.username)
                if (info.type == ContactType.PHONE)
                    set(UserExtraInfoTable.phone, info.contact.toLong())
                else
                    set(UserExtraInfoTable.email, info.contact)
                where {
                    UserExtraInfoTable.userId eq userId
                }
            }

        Database
            .delete(CandidateVerifyCodeTable) {
                it.userContact eq info.contact
            }

        call.respondJson(
            CodeResponse(
                hashAutoSignin(userId, info.password, now)
            )
        )
    }

    post("/verify"){
        val (username, contact, type) = call.receiveJson<ContactVerifyRequestArgument>()
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
                if (type == ContactType.PHONE)
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

    post("/signup/identify", setOf("X-User-Id", "X-User-Code")) {
        val user = call.request.headers["X-User-Id"]!!
            .toLongOrNull()
            ?: return@post call.respondErr("用户Id格式错误")

        if (!checkCode(
                user,
                call.request.headers["X-User-Code"] ?: ""
            )
        )
            return@post call.respondErr("用户身份验证失败")

        // todo human check
        val req = call.receiveJson<IdentityVerifyRequestArgument>()
            ?: return@post call.respondErr("请求参数格式错误")

        Database
            .insert(VerifyRequestTable) {
                set(it.userId, user)
                set(it.desireType, req.desireType)
                set(it.materials, req.materials.split(','))
            }
        call.respondOK()
    }
}
