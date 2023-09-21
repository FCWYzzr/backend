package com.yunchi.core.protocol.orm

import org.ktorm.entity.Entity
import java.time.Instant

enum class UserType{
    STUDENT,
    COMPANY,
    UNKNOWN,
    VISITOR
}

enum class GoodsType{
    OBJECT,
    FAVOUR,
    ANY
}

enum class IOType{
    SELLING,
    ACQUISITION,
    ANY
}

enum class ContactType {
    EMAIL,
    PHONE,
    QQ,
    WECHAT
}

interface UserIdentityModel: Entity<UserIdentityModel> {
    val id: Long
    val type: UserType
    val password: String
    val lastSignin: Instant
}

interface CandidateVerifyCodeModel: Entity<CandidateVerifyCodeModel> {
    val userContact: String
    val type: ContactType
    val code: String
    val expireAt: Instant
}

interface UserExtraInfoModel: Entity<UserExtraInfoModel> {
    val userId: Long
    val name: String
    val image: String
    val phone: Long
    val email: String
    val description: String
}

interface UserThirdPartyInfoModel : Entity<UserThirdPartyInfoModel> {
    val userId: Long
    val qq: Long
    val wechat: String
}

interface VerifyRequestModel : Entity<VerifyRequestModel> {
    val userId: Long
    val desireType: UserType
    val materials: List<String>
}


interface GoodsModel: Entity<GoodsModel>{
    val goodsId: Long
    val name: String
    val image: String

    val publishDate: Instant
    val validDate: Instant
    val money: Int
    val publisherId: Long

    val goodsType: GoodsType
    val ioType: IOType
    val publisherType: UserType
    val tags: List<String>
}

interface SnowflakeModel: Entity<SnowflakeModel>{
    val name: String
    val count: Int
}

interface GroupMessageModel: Entity<GroupMessageModel>{
    val id: Int
    val senderId: Long
    val groupId: Long
    val messageContent: String
    val time: Instant
}

interface GoodsGroupRedirectModel: Entity<GoodsGroupRedirectModel>{
    val goodsId: Long
    val groupId: Long
    val publisherId: Long
}

interface GroupReferenceModel: Entity<GroupReferenceModel>{
    val groupId: Long
    val reference: Int
}

interface GoodsAttributeModel: Entity<GoodsAttributeModel>{
    val id: Int
    val goodsId: Long
    val goodsFactor: String
    val success: Int
    val totalQuery: Int
}

interface AttributeInfoModel: Entity<AttributeInfoModel>{
    val attribute: String
    val count: Int
}