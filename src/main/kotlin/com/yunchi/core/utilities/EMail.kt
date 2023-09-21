package com.yunchi.core.utilities

import com.yunchi.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalTime
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


object EMailVerifier{
    private val config = Properties().apply {
        putAll(mapOf(
            "mail.smtp.auth" to "true",
            "mail.smtp.host" to Config.smtp.server,
            "mail.smtp.port" to Config.smtp.port
        ))
    }

    private val title = """
        请确认您的注册
    """.trimIndent()

    private val pattern = Files.readString(Path.of(
        Config.dirs.resource + Config.smtp.htmlPattern
    )).replace("[email]", Config.smtp.mail)
        .replace("[expireTime]", Config.verification.expireTime.toString())
        .replace("[timeUnit]", Config.verification.timeUnit.literal)

    private val auth = object :Authenticator(){
        override fun getPasswordAuthentication() =
            PasswordAuthentication(
                Config.smtp.mail,
                Config.smtp.password
            )
    }

    fun sendCodeAsync(name: String, contact: String, code: String){
        CoroutineScope(Dispatchers.IO).launch {
            sendCode(name, contact, code)
        }
    }

    private fun sendCode(name: String, contact: String, code: String){
        MimeMessage(Session.getInstance(config, auth)).apply {
            setFrom(InternetAddress(
                Config.smtp.mail,
                "云·集 管理员  Yunchi Manager",
                "UTF-8"
            ))
            setRecipient(Message.RecipientType.TO, InternetAddress(contact))
            subject = title

            val content = pattern
                .replace("[greeting]", currentGreeting())
                .replace("[name]", name)
                .replace("[code]", code)

            setContent(MimeMultipart().apply {
                addBodyPart(MimeBodyPart().apply {
                    setHeader("Content-Transfer-Encoding", "base64")
                    setContent(content, "text/html;charset=utf-8")
                })
            }

                ,
               "text/html;encoding=utf-8"
            )

            Transport.send(this)
        }
    }

    private fun currentGreeting(): String{
        return when(LocalTime.now()
            .hour){
            in 6..10 -> "早上好，"
            in 11..13 -> "中午好，"
            in 14..18 -> "下午好，"
            in 19..22 -> "晚上好，"
            else -> "来点夜生活吗，"
        }
    }
}