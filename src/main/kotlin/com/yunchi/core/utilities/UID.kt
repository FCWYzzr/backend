package com.yunchi.core.utilities

import com.yunchi.Config
import com.yunchi.Project
import com.yunchi.core.protocol.orm.Database
import com.yunchi.core.protocol.orm.SnowflakeTable
import com.yunchi.core.protocol.orm.firstOrNull
import org.ktorm.dsl.*
import java.util.BitSet
import java.util.UUID

private fun getCounter(name: String): Int{
    var counter = Database
        .from(SnowflakeTable)
        .select(SnowflakeTable.count)
        .where(
            SnowflakeTable.name eq name
        )
        .map { it[SnowflakeTable.count] }
        .firstOrNull() ?: run {
        Database
            .insert(SnowflakeTable) {
                set(it.name, name)
                set(it.count, 0)
            }
        0
    }

    Database
        .update(SnowflakeTable){
            set(SnowflakeTable.count, ++ counter)
            where {
                SnowflakeTable.name eq name
            }
        }

    return counter
}


/**
 * classic snowflake uid
 * signal + time + areaId + machineId + productId
 *   1    +  39  +  4     +    4      +   16
 */
fun genSnowflake(name: String): Long {
    val base = BitSet(64)
    var time = System.currentTimeMillis()

    var productId = getCounter(name)
    var area = Config.server.areaId
    var machine = Config.server.machineId

    // signal bit
    base[0] = false

    // time bit * 39
    for( i in 1..39 ){
        base[i] = ((time and 1) != 0L)
        time = time ushr 1
    }

    // area bit * 4
    if (area != 0)
        for( i in 40..43 ){
            base[i] = ((area and 1) != 0)
            area = area ushr 1
        }

    // machine bit * 4
    if (machine != 0)
        for( i in 44..47 ){
            base[i] = ((machine and 1) != 0)
            machine = machine ushr 1
        }

    // product bit * 16
    for( i in 48..63 ){
        base[i] = ((productId and 1) != 0)
        productId = productId ushr 1
    }

    return base.toLongArray()[0]
}

/**
 * classic uuid interface
 */
fun genUuid(): String {
    return UUID.randomUUID().toString()
}


