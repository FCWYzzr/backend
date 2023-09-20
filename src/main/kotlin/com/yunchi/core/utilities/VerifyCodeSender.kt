package com.yunchi.core.utilities

import com.yunchi.core.protocol.orm.ContactType

fun sendVerifyCode(username: String, contact: String, code: String, verifyType: ContactType) {
    when (verifyType) {
        ContactType.QQ -> EMailVerifier.sendCodeAsync(username, "$contact@qq.com", code)
        // todo add qq bot verifier
        ContactType.EMAIL -> EMailVerifier.sendCodeAsync(username, contact, code)
        ContactType.PHONE -> println(code) // todo add message verifier
        ContactType.WECHAT -> println(code) // todo add wechat bot verifier
    }
}