package com.yunchi.core.utilities

import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.Instant
import java.util.Calendar

@Serializable
enum class TimeUnit(
    val multiplier: Int,
    val literal: String
){
    SECOND(1, "秒"),
    MINUTE(60 * SECOND.multiplier, "分钟"),
    HOUR(60 * MINUTE.multiplier, "小时"),
    DAY(24 * HOUR.multiplier, "天"),
    MONTH(30 * DAY.multiplier, "月"),
    YEAR(365 * DAY.multiplier, "年")
}

fun lastFor(last: Long, unit: TimeUnit): Instant{
    return Instant.ofEpochSecond(
        Instant.now().epochSecond
        + last * unit.multiplier
    )
}