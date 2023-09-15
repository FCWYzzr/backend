package com.yunchi.core.goods_system

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.itemSystem(){
    routing {
        configureBridge()
        configurePublish()
        configureQuery()
    }
}