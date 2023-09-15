package com.yunchi.core.goods_system

import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.GoodsGroupRedirectTable
import com.yunchi.core.protocol.orm.GroupMessageTable
import com.yunchi.core.user_system.checkCode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ktorm.dsl.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.filterNotNull
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.set

val RecordGroup = ConcurrentHashMap<
    Long,
    ConcurrentHashMap<WebSocketServerSession, Unit>
    >()
fun Route.configureBridge(){
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

    webSocket("/group"){
        var group: Long? = null
        try{
            val rawMsg = incoming.readNext()
            val info = Json.decodeFromString<MessageQueryArgument>(rawMsg)
            group = info.groupId

            if (!checkCode(info.userId, info.code))
                return@webSocket close(CloseReason(
                    CloseReason.Codes.VIOLATED_POLICY,
                    "Invalid Code"
                ))

            RecordGroup[group]?.set(this, Unit)
                ?: run {
                    RecordGroup[group] = ConcurrentHashMap()
                    RecordGroup[group]!![this] = Unit
                }

            val timestamp = Instant.ofEpochSecond(info.fromTime ?: 0)

            val messages = Database
                .from(GroupMessageTable)
                .select(
                    GroupMessageTable.senderId,
                    GroupMessageTable.messageContent,
                    GroupMessageTable.time
                )
                .where(
                    (GroupMessageTable.groupId eq group) and
                        (GroupMessageTable.time gt timestamp)
                ).orderBy( GroupMessageTable.id.asc() )
                .map { MessageChunk(
                    it[GroupMessageTable.senderId]!!,
                    it[GroupMessageTable.messageContent]!!,
                    it[GroupMessageTable.time]!!.epochSecond
                ) }

            messages.forEach {
                send(Json.encodeToString(it))
            }

            for ( frame in incoming ){
                val message = Json.decodeFromString<MessageArgument>(
                    (frame as Frame.Text).readText()
                )

                CoroutineScope(Dispatchers.IO).launch {
                    Database
                        .insertAndGenerateKey(GroupMessageTable){
                            set(it.senderId, info.userId)
                            set(it.groupId, info.groupId)
                            set(it.time, Instant.ofEpochSecond(message.time))
                            set(it.messageContent, message.content)
                        }
                }

                RecordGroup[group]!!
                    .keys()
                    .asSequence()
                    .filter { it != this }
                    .forEach{
                        it.send(Json.encodeToString(
                            MessageChunk(
                                info.userId,
                                message.content,
                                message.time
                            )
                        ))
                    }
            }
        }
        catch (ignored: ClosedWriteChannelException){}
        catch (e: Throwable){
            e.printStackTrace()
        }
        finally {
            if (group != null)
                RecordGroup[group]!!.remove(this)
        }

    }
}

private suspend fun ReceiveChannel<Frame>.readNext(): String{
    val ret = receive()
    return (ret as Frame.Text).readText()
}

