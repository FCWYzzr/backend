package com.yunchi.core.user_system

import com.yunchi.Project.emailTemplate
import com.yunchi.Project.phoneTemplate
import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.utilities.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import java.time.Instant

fun Routing.configureSignUp(){
    post("/verify"){
        val (username, contact) = call.receiveJson<VerifyRequestArgument>()
            ?: return@post call.respondErr("invalid request")

        val sender: (name: String, contact: String, String) -> Unit
        val type = when {
            contact matches phoneTemplate  -> {
                sender = {_, _, _ ->}
                VerifyType.PHONE
            }
            contact matches emailTemplate -> {
                sender = EMailVerifier::sendCodeAsync
                VerifyType.EMAIL
            }
            else -> return@post call.respondErr("unknown contact type")
        }

        val code = VerifyCode.newCode(contact, type)
            ?: return@post call.respondErr("already exist")

        sender.invoke(username, contact, code)
        call.respond(HttpStatusCode.OK)
    }

    post("/signup"){
        val info = call.receiveJson<SignUpArgument>()
            ?: return@post call.respondErr("invalid request")

        val type = VerifyCode.matchCode(info.contact, info.code)
            ?: return@post call.respondErr("not verified")

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
