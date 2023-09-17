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
import org.ktorm.schema.ColumnDeclaring
import java.time.Instant
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.log10


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
        val existKeywordsInfo = if (query.keywords.isNotEmpty())
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
            null

        val keyFiltered = if (query.keywords.isNotEmpty())
            Database
            .from(GoodsAttributeTable)
            .select()
                .where(GoodsAttributeTable.goodsFactor inList query.keywords)
                .map {
                it[GoodsAttributeTable.goodsId]!! to
                    mutableListOf(it[GoodsAttributeTable.goodsFactor]!!)
                }
                .map {
                hashMapOf(it)
                }
                .stream().parallel()
            .reduce(HashMap()){ m1, m2 ->
                for ((k, v) in m2) {
                    if (k in m1.keys)
                        m1[k]!!.addAll(v)
                    else
                        m1[k] = v
                }
                m1
            }
        else
            null

        val queryFiltered = Database
            .from(GoodsTable)
            .select()
            .where {
                val cond = mutableListOf<ColumnDeclaring<Boolean>>(
                    GoodsTable.validDate gt Instant.now()
                )
                if (keyFiltered != null)
                    cond.add(GoodsTable.id inList keyFiltered.keys)

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
            .map { it[GoodsTable.id]!! to it[GoodsTable.publishDate]!! }
            .toMap()

        val now = Instant.now()

        val filtered = if (keyFiltered != null)
            keyFiltered
                .filterKeys { it in queryFiltered.keys }
                .entries.parallelStream()
                .map { (goodsId, keywords) ->
                    goodsId to Database
                        .from(GoodsAttributeTable)
                        .select()
                        .where(
                            (GoodsAttributeTable.goodsId eq goodsId) and
                                (GoodsAttributeTable.goodsFactor inList keywords)
                        )
                        .map {
                            it[GoodsAttributeTable.goodsFactor]!! to
                                (it[GoodsAttributeTable.success]!! * 1F
                                    / it[GoodsAttributeTable.success]!!
                                    / existKeywordsInfo!![it[GoodsAttributeTable.goodsFactor]!!]!!
                                    * 100
                                    + queryFiltered[it[GoodsAttributeTable.goodsId]!!]!!.epochSecond
                                    - now.epochSecond)
                        }
                        .toMap()
                }
                .map { it.first to (it.second.values.sum() to it.second) }
                .sorted { p1, p2 -> p2.second.first.compareTo(p1.second.first) }
        else
            queryFiltered
                .entries.parallelStream()
                .map { it.key to log10(now.epochSecond - it.value.epochSecond * 1F) }
                .sorted { p1, p2 -> p1.second.compareTo(p2.second) }
                .map { it.first to (it.second to mapOf("time-delta" to it.second)) }

        call.respondJson(buildJsonArray {
            filtered.forEach { (id, scores) ->
                val (score, part) = scores
                add(buildJsonObject {
                    put("goodsId", JsonPrimitive(id))
                    put("score", JsonPrimitive(score))
                    put("keywords", buildJsonObject {
                        part.forEach { (k, v) ->
                            put(k, JsonPrimitive(v))
                        }
                    })
                })
            }
        })
    }
}
