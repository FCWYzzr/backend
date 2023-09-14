package com.yunchi.core.protocol.orm

import com.yunchi.Project
import org.ktorm.database.Database
import org.ktorm.schema.*

val Database: Database
    get() = Project.database


object CandidateVerifyCodeTable:
    Table<CandidateVerifyCodeModel>("candidate_verify"){

    val userContact = text("contact")
        .primaryKey()
        .bindTo(CandidateVerifyCodeModel::userContact)

    val type = enumOf("type", VerifyType::valueOf)
        .bindTo(CandidateVerifyCodeModel::type)

    val code = text("code")
        .bindTo(CandidateVerifyCodeModel::code)

    val expireAt = timestamp("expire_at")
        .bindTo(CandidateVerifyCodeModel::expireAt)
}

object UserIdentityTable:
    Table<UserIdentityModel>("user_identity"){

    val id = long("id")
        .primaryKey()
        .bindTo(UserIdentityModel::id)

    val type = enumOf("type", UserType::valueOf)
        .bindTo(UserIdentityModel::type)

    val pwd = text("password")
        .bindTo(UserIdentityModel::password)

    val lastSignin = timestamp("last_signin")
        .bindTo(UserIdentityModel::lastSignin)
}


object UserExtraInfoTable:
    Table<UserExtraInfoModel>("user_extra_info"){

    val userId = long("user_id")
        .primaryKey()
        .bindTo(UserExtraInfoModel::userId)

    val name = text("name")
        .bindTo(UserExtraInfoModel::name)

    val phone = long("phone")
        .bindTo(UserExtraInfoModel::phone)

    val email = text("email")
        .bindTo(UserExtraInfoModel::email)

    val description = text("description")
        .bindTo(UserExtraInfoModel::description)
}

object GroupMessageTable:
    Table<GroupMessageModel>("group_message"){

    val id = int("id")
        .primaryKey()
        .bindTo(GroupMessageModel::id)

    val senderId = long("sender_id")
        .bindTo(GroupMessageModel::senderId)

    val groupId = long("group_id")
        .bindTo(GroupMessageModel::groupId)

    val messageContent = text("message")
        .bindTo(GroupMessageModel::messageContent)

    val time = timestamp("time")
        .bindTo(GroupMessageModel::time)
}

object GoodsTable:
    Table<GoodsModel>("record_goods"){

    val id = long("id")
        .primaryKey()
        .bindTo(GoodsModel::goodsId)
    val name = text("name")
        .bindTo(GoodsModel::name)
    val validDate = timestamp("valid_date")
        .bindTo(GoodsModel::validDate)
    val money = int("money")
        .bindTo(GoodsModel::money)
    val publisherId = long("publisher_id")
        .bindTo(GoodsModel::publisherId)

    val goodsType = enumOf("goods_type", GoodsType::valueOf)
        .bindTo(GoodsModel::goodsType)
    val ioType = enumOf("io_type", IOType::valueOf)
        .bindTo(GoodsModel::ioType)
    val publisherType = enumOf("publisher_type", UserType::valueOf)
        .bindTo(GoodsModel::publisherType)

    val tags = list("tags"){it}
        .bindTo(GoodsModel::tags)
}

object GoodsGroupRedirectTable:
    Table<GoodsGroupRedirectModel>("goods_group"){
    val goodsId = long("goods_id")
        .primaryKey()
        .bindTo(GoodsGroupRedirectModel::goodsId)

    val groupId = long("group_id")
        .bindTo(GoodsGroupRedirectModel::groupId)

    val publisherId = long("publisher_id")
        .bindTo(GoodsGroupRedirectModel::publisherId)
}

object GroupReferenceTable:
    Table<GroupReferenceModel>("group_reference"){

    val groupId = long("group_id")
        .primaryKey()
        .bindTo(GroupReferenceModel::groupId)

    val reference = int("reference")
        .bindTo(GroupReferenceModel::reference)
}

object GoodsAttributeTable:
    Table<GoodsAttributeModel>("goods_attr"){

    val id = int("id")
        .primaryKey()
        .bindTo(GoodsAttributeModel::id)

    val goodsId = long("goods_id")
        .bindTo(GoodsAttributeModel::goodsId)
    val goodsFactor = text("goods_factor")
        .bindTo(GoodsAttributeModel::goodsFactor)

    val success = int("fit_count")
        .bindTo(GoodsAttributeModel::success)
    val totalQuery = int("query_count")
        .bindTo(GoodsAttributeModel::totalQuery)
}

object AttributeInfoTable:
    Table<AttributeInfoModel>("attr_info"){

    val attribute = text("attribute")
        .primaryKey()
        .bindTo(AttributeInfoModel::attribute)
    val count = int("count")
        .bindTo(AttributeInfoModel::count)
}

object SnowflakeTable:
    Table<SnowflakeModel>("snowflake"){

    val name = text("name")
        .primaryKey()
        .bindTo(SnowflakeModel::name)

    val count = int("count")
        .bindTo(SnowflakeModel::count)
}

