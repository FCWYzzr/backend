package com.yunchi.core.protocol


import com.yunchi.core.protocol.orm.GoodsType
import com.yunchi.core.protocol.orm.IOType
import com.yunchi.core.protocol.orm.UserType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SignInArgument(
    val identifier: String,
    val password: String
)

@Serializable
data class AutoSignInArgument(
    val userId: Long,
    val code: String
)

@Serializable
data class VerifyRequestArgument(
    val username: String,
    val contact: String
)

@Serializable
data class SignUpArgument(
    val username: String,
    val password: String,
    val contact: String,
    val code: String
)

@Serializable
data class MessageQueryArgument(
    val userId: Long,
    val code: String,
    val groupId: Long,
    val fromTime: Long?
)

@Serializable
data class MessageArgument(
    val content: String,
    val time: Long
)

@Serializable
data class PublishArgument(
    val goodsName: String,
    val validDate: Long,
    val money: Int,
    val keywords: List<String>,

    val goodsType: GoodsType,
    val ioType: IOType,
    val tags: String
)

@Serializable
data class QueryArgument(
    val keywords: List<String>?,
    val minCost: Int,
    val maxCost: Int,
    val publisher: UserType?,
    val type: GoodsType?,
    val perPage: Int,
    val page: Int
){companion object {
    fun of(param: Parameters): QueryArgument = QueryArgument(
        param["keywords"]?.split(";"),
        param["maxCost"].orEmpty().toIntOrNull() ?: 0,
        param["minCost"].orEmpty().toIntOrNull() ?: Int.MAX_VALUE,
        if (param["publisher"] != null)
            UserType.valueOf(param["publisher"]!!)
        else
            null,
        if (param["type"] != null)
            GoodsType.valueOf(param["type"]!!)
        else
            null,
        param["perPage"]?.toIntOrNull() ?: 30,
        param["page"]?.toIntOrNull() ?: 0
    )
}}

suspend inline fun <reified T> ApplicationCall.receiveJson(): T?{
    return try{
        Json.decodeFromString<T>(this.receiveText())
    }catch (e: Throwable){
        e.printStackTrace()
        null
    }
}

