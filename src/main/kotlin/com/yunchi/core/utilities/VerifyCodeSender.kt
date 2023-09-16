package com.yunchi.core.utilities

import com.yunchi.core.protocol.orm.VerifyType

fun sendVerifyCode(username: String, contact: String, code: String, verifyType: VerifyType) {
    when (verifyType) {
        VerifyType.QQ -> EMailVerifier.sendCodeAsync(username, "$contact@qq.com", code)
        // todo add qq bot verifier
        VerifyType.EMAIL -> EMailVerifier.sendCodeAsync(username, contact, code)
        VerifyType.PHONE -> println(code) // todo add message verifier
        VerifyType.WECHAT -> println(code) // todo add wechat bot verifier
    }
}