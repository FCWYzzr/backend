package com.yunchi.core.item_system

import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.itemSystem(){
    routing {
        configureBridge()
        configurePublish()
        configureQuery()
    }
}