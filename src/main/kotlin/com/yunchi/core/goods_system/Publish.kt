@file:Suppress("DuplicatedCode")

package com.yunchi.core.goods_system

import com.yunchi.core.protocol.*
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.user_system.checkCode
import com.yunchi.core.utilities.DelegatedRouterBuilder
import com.yunchi.core.utilities.genSnowflake
import io.ktor.server.application.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ktorm.dsl.*
import java.time.Instant

fun DelegatedRouterBuilder.configurePublish() {
    post("/goods/publish", setOf("X-User-Id", "X-User-Code")) {
        val userId = call.request.headers["X-User-Id"]
            .orEmpty().toLongOrNull()
            ?: return@post call.respondErr("X-User-Id格式错误")
        val code = call.request.headers["X-User-Code"]!!

        val publish = call.receiveJson<PublishArgument>()
            ?: return@post call.respondErr("请求格式错误")

        val keywords = publish.keywords.split(';')
        val tags = publish.tags.split(';')

        if (!checkCode(userId, code))
            return@post call.respondErr("无效的用户登录")

        val goodId = genSnowflake("goods")
        val publisherType = Database
            .from(UserIdentityTable)
            .select(UserIdentityTable.type)
            .where(UserIdentityTable.id eq userId)
            .map { it[UserIdentityTable.type]!! }
            .firstOrNull()?: return@post call.respondErr("不存在该用户")

        if (publisherType == UserType.UNKNOWN)
            return@post call.respondErr("未认证会员不可发布需求")

        Database
            .insert(GoodsTable){
                set(it.id, goodId)
                set(it.name, publish.goodsName)
                set(it.money, publish.money)

                set(it.publishDate, Instant.ofEpochSecond(publish.publishDate))
                set(it.validDate, Instant.ofEpochSecond(publish.validDate))
                set(it.goodsType, publish.goodsType)
                set(it.ioType, publish.ioType)

                set(it.publisherId, userId)
                set(it.publisherType, publisherType)
                set(it.tags, tags)

                set(it.image, publish.goodsImage)
            }

        CoroutineScope(Dispatchers.IO).launch{
            Database.batchInsert(GoodsAttributeTable){
                for (attr in keywords)
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
                    AttributeInfoTable.attribute inList keywords
                }
                .map {
                    it[AttributeInfoTable.attribute]!!
                }
                .toSet()

            Database.batchInsert(AttributeInfoTable){
                for (attr in keywords - existKeywords)
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

    delete("/goods/remove", setOf("X-User-Id", "X-User-Code")) {
        val user = call.request.headers["X-User-Id"]
            .orEmpty().toLongOrNull()
            ?: return@delete call.respondErr("X-User-Id格式错误")

        val code = call.request.headers["X-User-Code"]!!

        if (! checkCode(user, code))
            return@delete call.respondErr("用户认证失败")

        val goodsId = call.receiveJson<GoodsRemoveArgument>()?.goodsId
            ?: return@delete call.respondErr("请求参数格式错误")

        val img = Database
            .from(GoodsTable)
            .select(GoodsTable.image)
            .where {
                GoodsTable.id eq goodsId
            }.map {
                it[GoodsTable.image]!!
            }.firstOrNull() ?: return@delete call.respondErr("商品不存在")


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
            return@delete call.respondJson(UrlResponse(img))
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

        call.respondJson(UrlResponse(img))
    }
    delete("/goods/complete"){
        // todo
    }
}