@file:Suppress("DuplicatedCode")

package com.yunchi.core.item_system

import com.yunchi.Config
import com.yunchi.configure
import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.user_system.checkCode
import com.yunchi.core.utilities.genSnowflake
import com.yunchi.dirIfNotExist
import com.yunchi.fileIfNotExist
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ktorm.dsl.*
import java.io.File
import java.time.Instant

fun Route.configurePublish(){
    post("/goods/publish"){
        call.response.configure("X-User-Id", "X-User-Code")

        val userId = call.request.headers["X-User-Id"]
            .orEmpty().toLongOrNull()
            ?: return@post call.respondErr("User Id Invalid")
        val code = call.request.headers["X-User-Code"]!!

        val publish = call.receiveJson<PublishArgument>()
            ?: return@post call.respondErr("Invalid Request")

        if (!checkCode(userId, code))
            return@post call.respondErr("Invalid User Login")

        val goodId = genSnowflake("goods")
        val publisherType = Database
            .from(UserIdentityTable)
            .select(UserIdentityTable.type)
            .where(UserIdentityTable.id eq userId)
            .map { it[UserIdentityTable.type]!! }
            .firstOrNull()?:
            return@post call.respondErr("No Such User")

        if (publisherType == UserType.UNKNOWN)
            return@post call.respondErr("Only Student & Company can publish goods")

        Database
            .insert(GoodsTable){
                set(it.id, goodId)
                set(it.name, publish.goodsName)
                set(it.money, publish.money)

                set(it.validDate, Instant.ofEpochSecond(publish.validDate))
                set(it.goodsType, publish.goodsType)
                set(it.ioType, publish.ioType)

                set(it.publisherId, userId)
                set(it.publisherType, publisherType)
                set(it.tags, publish.tags.split(";"))
            }
        CoroutineScope(Dispatchers.IO).launch{
            Database.batchInsert(GoodsAttributeTable){
                for(attr in publish.keywords)
                    item {
                        set(it.goodsId, goodId)
                        set(it.goodsFactor, attr)
                        set(it.success, 1)
                        set(it.totalQuery, 1)
                    }
            }
            val existKeywords = Database
                .from(AttributeInfoTable)
                .select()
                .where{
                    AttributeInfoTable.attribute inList publish.keywords
                }
                .map {
                    it[AttributeInfoTable.attribute]!!
                }
                .toSet()

            Database.batchInsert(AttributeInfoTable){
                for(attr in publish.keywords - existKeywords)
                    item {
                        set(it.attribute, attr)
                        set(it.count, 1)
                    }
            }

            Database.batchUpdate(AttributeInfoTable){
                for(attr in existKeywords)
                    item {
                        set(it.count, it.count + 1)
                        where {
                            it.attribute eq attr
                        }
                    }
            }
        }

        Database
            .insert(GoodsGroupRedirectTable){
                set(it.goodsId, goodId)
                set(it.publisherId, userId)
                set(it.groupId, goodId)
            }

        Database
            .insert(GroupReferenceTable){
                set(it.groupId, goodId)
                set(it.reference, 0)
            }

        call.respondJson(GoodsResponse(goodId))
    }
    put("/goods/icon"){
        call.response.configure("X-Goods-Id", "X-User-Id", "X-User-Code")
        val user = call.request.headers["X-User-Id"]!!.toLongOrNull() ?: return@put call.respondErr(
            "invalid userId"
        )
        val code = call.request.headers["X-User-Code"]!!
        val goodsId = call.request.headers["X-Goods-Id"]!!
            .toLongOrNull()
            ?: return@put call.respondErr("invalid header")

        val seller = Database
            .from(GoodsTable)
            .select()
            .where(GoodsTable.id eq goodsId)
            .mapNotNull { it[GoodsTable.publisherId] }
            .firstOrNull()
            ?: return@put call.respondErr("goods not exist")

        if (seller != user)
            return@put call.respondErr("operation denied")

        if (!checkCode(user, code))
            return@put call.respondErr("user verify fail")

        dirIfNotExist("${Config.resource}goods/icon/")

        val os = fileIfNotExist("${Config.resource}goods/icon/$goodsId.pic")
            .outputStream()

        withContext(Dispatchers.IO) {
            call.receiveStream().transferTo(os)
        }

        os.close()

        call.respond(HttpStatusCode.OK)
    }
    get("/goods/icon"){
        call.response.configure()
        val goodsId = call.parameters["goodsId"].orEmpty()
            .toLongOrNull()
            ?: return@get call.respondErr(
                "invalid header"
            )

        val file = File("${Config.resource}goods/icon/$goodsId.pic")
        if(file.exists())
            call.respondFile(file)
        else
            call.respond(HttpStatusCode.NotFound)
    }
    delete("/goods/remove"){
        call.response.configure("X-User-Id", "X-User-Code")
        val user = call.request.headers["X-User-Id"]
            .orEmpty().toLongOrNull()
            ?: return@delete call.respondErr("invalid user id")

        val code = call.request.headers["X-User-Code"]!!

        if (! checkCode(user, code))
            return@delete call.respondErr("user verify fail")

        val goodsId = call.receiveJson<GoodsRemoveArgument>()?.goodsId
            ?: return@delete call.respondErr("Invalid request")

        val file = File("${Config.resource}goods/icon/$goodsId.pic")
        if(file.exists())
            file.delete()

        Database
            .delete(GoodsTable){
                it.id eq goodsId
            }

        Database
            .delete(GoodsAttributeTable){
                it.goodsId eq goodsId
            }

        val group = Database
            .from(GoodsGroupRedirectTable)
            .select()
            .where {
                GoodsGroupRedirectTable.goodsId eq goodsId
            }.map {
                it[GoodsGroupRedirectTable.groupId]!!
            }.first()

        val count = Database
            .from(GroupReferenceTable)
            .select(GroupReferenceTable.reference)
            .where(GroupReferenceTable.groupId eq group).map{
                it[GroupReferenceTable.reference]!!
            }.first()

        Database
            .delete(GoodsGroupRedirectTable){
                GoodsGroupRedirectTable.goodsId eq goodsId
            }

        if (count > 1){
            Database
                .update(GroupReferenceTable){
                    set(GroupReferenceTable.reference, count - 1)
                    where { GroupReferenceTable.groupId eq group }
                }
            return@delete
        }

        Database
            .delete(GroupReferenceTable){
                GroupReferenceTable.groupId eq group
            }

        Database.delete(GroupMessageTable){
            GroupMessageTable.groupId eq group
        }

        if (RecordGroup.containsKey(group)){
            val msg = Json.encodeToString(MessageChunk(
                -1, "商品已下架", Instant.now().epochSecond
            ))
            RecordGroup[group]!!.forEach {
                it.key.send(msg)
            }
            RecordGroup.remove(group)
        }

        call.respond(HttpStatusCode.OK)
    }
    delete("/goods/complete"){
        // todo
    }
}