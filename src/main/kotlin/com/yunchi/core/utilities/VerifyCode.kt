package com.yunchi.core.utilities

import com.yunchi.Config
import com.yunchi.Project
import com.yunchi.core.protocol.orm.*
import org.ktorm.dsl.*
import java.time.Instant


object VerifyCode {
    private val config: Project.Config.VerificationConfig
        get() = Config.verification

    private fun genCode(): String {
        return Project.randGen.nextInt(100_000, 1_000_000).toString()
    }

    private fun contains(contact: String): Boolean{
        return Database
            .from(CandidateVerifyCodeTable)
            .select()
            .where(
                CandidateVerifyCodeTable.userContact eq contact
            )
            .firstOrNull() != null
    }

    private fun updateCode(contact: String, code: String, verifyType: VerifyType){
        Database
            .update(CandidateVerifyCodeTable){
                set(it.code, code)
                set(it.type, verifyType)
                set(it.expireAt, lastFor(config.expireTime, config.timeUnit))
                where { it.userContact eq contact }
            }

    }

    private fun saveCode(contact: String, code: String, verifyType: VerifyType){
        Database
            .insert(CandidateVerifyCodeTable){
                set(it.code, code)
                set(it.type, verifyType)
                set(it.expireAt, lastFor(config.expireTime, config.timeUnit))
                set(it.userContact, contact)
            }
    }

    private fun alreadyRegister(contact: String, contactType: VerifyType): Boolean{
        return when (contactType) {
            VerifyType.EMAIL, VerifyType.PHONE -> Database
                .from(UserExtraInfoTable)
                .select()
                .where(
                    if (contactType == VerifyType.EMAIL)
                        UserExtraInfoTable.email eq contact
                    else
                        UserExtraInfoTable.phone eq contact.toLong()
                )
                .firstOrNull() != null

            VerifyType.QQ, VerifyType.WECHAT -> Database
                .from(UserThirdPartyInfoTable)
                .select()
                .where(
                    if (contactType == VerifyType.QQ)
                        UserThirdPartyInfoTable.qq eq contact.toLong()
                    else
                        UserThirdPartyInfoTable.wechat eq contact
                )
                .firstOrNull() != null
        }
    }

    fun newCode(contact: String, contactType: VerifyType): String? {
        if (alreadyRegister(contact, contactType))
            return null
        val code = genCode()

        if (contains(contact))
            updateCode(contact, code, contactType)
        else
            saveCode(contact, code, contactType)

        return code
    }

    fun matchCode(contact: String, code: String): VerifyType?{
        val current = Instant.now()
        return Database
            .from(CandidateVerifyCodeTable)
            .select(CandidateVerifyCodeTable.type)
            .where{
                (CandidateVerifyCodeTable.userContact eq contact) and
                    (CandidateVerifyCodeTable.code eq code) and
                    (CandidateVerifyCodeTable.expireAt gt current)
            }
            .map { it[CandidateVerifyCodeTable.type] }
            .firstOrNull()
    }
}