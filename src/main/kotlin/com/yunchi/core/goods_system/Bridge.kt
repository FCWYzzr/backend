package com.yunchi.core.goods_system

import com.yunchi.core.protocol.GroupResponse
import com.yunchi.core.protocol.SellerResponse
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.GoodsGroupRedirectTable
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.utilities.DelegatedRouterBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import org.ktorm.dsl.*
import java.util.concurrent.ConcurrentHashMap

val RecordGroup = ConcurrentHashMap<
    Long,
    ConcurrentHashMap<WebSocketServerSession, Unit>
    >()

fun DelegatedRouterBuilder.configureBridge() {
    get("/group/redirect"){
        val goodsId = call.parameters["goodsId"]
            .orEmpty().toLongOrNull()
            ?: return@get call.respondErr("Invalid Request")

        val groupId = Database
            .from(GoodsGroupRedirectTable)
            .select(GoodsGroupRedirectTable.goodsId)
            .where (GoodsGroupRedirectTable.goodsId eq goodsId)
            .map { it[GoodsGroupRedirectTable.goodsId]!! }
            .firstOrNull()
            ?: return@get call.respondErr("Goods not found", HttpStatusCode.NotFound)

        call.respondJson(GroupResponse(groupId))
    }

    get("/group/seller"){
        val groupId = call.parameters["groupId"]
            .orEmpty().toLongOrNull()
            ?: return@get call.respondErr("Invalid Request")

        val seller = Database
            .from(GoodsGroupRedirectTable)
            .select(GoodsGroupRedirectTable.publisherId)
            .where {
                GoodsGroupRedirectTable.goodsId eq groupId
            }
            .map { it[GoodsGroupRedirectTable.publisherId] }
            .filterNotNull()
            .firstOrNull()
            ?: return@get call.respondErr("Group not found", HttpStatusCode.NotFound)

        call.respondJson(SellerResponse(seller))
    }
}


