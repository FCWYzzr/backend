package com.yunchi.core.goods_system

import com.yunchi.core.protocol.GoodsDetail
import com.yunchi.core.protocol.QueryArgument
import com.yunchi.core.protocol.orm.*
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import com.yunchi.core.utilities.DelegatedRouterBuilder
import io.ktor.server.application.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.ktorm.dsl.*
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set


fun DelegatedRouterBuilder.configureQuery() {
    get("/query/info"){
        val ids = call.parameters["goodsIds"]
            ?: return@get call.respondErr("请求参数缺失商品Id")

        val idList = ids.split(";")
            .mapNotNull(String::toLongOrNull)

        call.respondJson(
            Database
                .from(GoodsTable)
                .select()
                .where (
                    (GoodsTable.id inList idList) and
                        (GoodsTable.validDate gt Instant.now())
                ).map { it[GoodsTable.id] to GoodsDetail.of(it) }
                .toMap()
        )
    }
    get("/query"){
        val query = QueryArgument.of(call.parameters)
        val existKeywordsInfo = if (query.keywords.isEmpty())
            Database
            .from(AttributeInfoTable)
            .select()
            .where (
                AttributeInfoTable.attribute inList query.keywords
            )
            .map {
                it[AttributeInfoTable.attribute]!! to
                it[AttributeInfoTable.count]!!
            }
            .toMap()
        else
            Database
                .from(AttributeInfoTable)
                .select()
                .map {
                    it[AttributeInfoTable.attribute]!! to
                        it[AttributeInfoTable.count]!!
                }
                .toMap()

        val keyFiltered = Database
            .from(GoodsAttributeTable)
            .select()
            .where(
                if (query.keywords.isNotEmpty())
                    GoodsAttributeTable.goodsFactor inList query.keywords
                else
                    GoodsAttributeTable.goodsFactor.isNotNull()
            ).map {
                it[GoodsAttributeTable.goodsId]!! to
                    mutableListOf(it[GoodsAttributeTable.goodsFactor]!!)
            }.map {
                hashMapOf(it)
            }.stream().parallel()
            .reduce(HashMap()){ m1, m2 ->
                for ((k, v) in m2){
                    if (k in m1.keys)
                        m1[k]!!.addAll(v)
                    else
                        m1[k] = v
                }
                m1
            }

        val queryFiltered = Database
            .from(GoodsTable)
            .select()
            .where {
                val cond = mutableListOf(
                    GoodsTable.id inList keyFiltered.keys,
                    GoodsTable.validDate gt Instant.now()
                )
                if (query.goodsType != GoodsType.ANY)
                    cond.add(GoodsTable.goodsType eq query.goodsType)
                if (query.ioType != IOType.ANY)
                    cond.add(GoodsTable.ioType eq query.ioType)
                if (query.tags.isNotEmpty())
                    query.tags.forEach {
                        cond.add(GoodsTable.tags like "%$it%")
                    }
                if (query.goodsType != GoodsType.ANY)
                    cond.add(GoodsTable.goodsType eq query.goodsType)

                cond.reduce { c1, c2 -> c1 and c2 }
            }
            .limit(
                query.perPage * query.page,
                query.perPage
            ).asIterable()
            .map { it[GoodsTable.id] }
            .toSet()

        val filtered = keyFiltered
            .filterKeys { it in queryFiltered }
            .entries.parallelStream()
            .map {(goodsId, keywords) ->
                goodsId to Database
                    .from(GoodsAttributeTable)
                    .select()
                    .where (
                        (GoodsAttributeTable.goodsId eq goodsId) and
                        (GoodsAttributeTable.goodsFactor inList keywords)
                    )
                    .map {
                        it[GoodsAttributeTable.goodsFactor]!! to
                            ( it[GoodsAttributeTable.success]!! * 1F
                                / it[GoodsAttributeTable.success]!!
                                / existKeywordsInfo[it[GoodsAttributeTable.goodsFactor]!!]!!)
                    }
                    .toMap()
            }



        call.respondJson(buildJsonArray {
            filtered.forEach { (k, v) ->
                add(buildJsonObject {
                    put("goodsId", JsonPrimitive(k))
                    put("score", JsonPrimitive(v.values.sum()))
                    put("keywords", buildJsonObject {
                        v.forEach{(k, v) ->
                            put(k, JsonPrimitive(v))
                        }
                    })
                })
            }
        })
    }
}