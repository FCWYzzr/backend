package com.yunchi.core.utilities


import com.yunchi.Config
import com.yunchi.Project
import java.security.MessageDigest
import java.time.Instant

fun hashRaw(text: String): ByteArray{
    val inst = MessageDigest
        .getInstance(Config.hashMethod)

    inst.update(text.toByteArray())

    return inst.digest()
}

@OptIn(ExperimentalStdlibApi::class)
fun hashString(text: String): String{
    return hashRaw(text).toHexString()
}

fun hashAutoSigninParted(userid: Long, password: String): (now: Instant) -> String {
    return {now ->
        hashString("$userid$password${now.epochSecond}")
    }
}

fun hashAutoSignin(userid: Long, password: String, now: Instant): String{
    return  hashString("$userid$password${now.epochSecond}")
}


