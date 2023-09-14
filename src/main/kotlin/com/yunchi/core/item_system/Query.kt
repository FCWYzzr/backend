package com.yunchi.core.item_system

import com.yunchi.configure
import com.yunchi.core.protocol.GoodsDetail
import com.yunchi.core.protocol.QueryArgument
import com.yunchi.core.protocol.orm.AttributeInfoTable
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.GoodsAttributeTable
import com.yunchi.core.protocol.orm.GoodsTable
import com.yunchi.core.protocol.respondErr
import com.yunchi.core.protocol.respondJson
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.ktorm.dsl.*
import java.time.Instant


fun Route.configureQuery(){
    get("/query/info"){
        call.response.configure()
        val ids = call.parameters["goodsIds"]
            ?: return@get call.respondErr("Invalid Request")

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
        call.response.configure()
        val query = QueryArgument.of(call.parameters)
        val existKeywordsInfo = if (query.keywords != null)
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

        val filtered = Database
            .from(GoodsAttributeTable)
            .select()
            .where (
                if(query.keywords != null)
                    GoodsAttributeTable.goodsFactor inList query.keywords
                else
                    GoodsAttributeTable.goodsFactor.isNotNull()
            ).asIterable()
            .map {
                 it[GoodsAttributeTable.goodsId]!! to
                     mutableListOf(it[GoodsAttributeTable.goodsFactor]!!)
            }
            .parallelStream()
            .map{ mutableMapOf(it) }
            .reduce(HashMap()){ m1, m2 ->
                for ((k, v) in m2){
                    if (k in m1.keys)
                        m1[k]!!.addAll(v)
                    else
                        m1[k] = v
                }
                m1
            }
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
            .sorted{p1, p2 ->
                p1.second.values.sum().compareTo(p2.second.values.sum())
            }
        call.respondJson(buildJsonArray {
            filtered.forEach{(k, v) ->
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