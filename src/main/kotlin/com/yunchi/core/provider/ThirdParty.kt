package com.yunchi.core.provider

import com.yunchi.Project.Config.ThirdPartyConfig.TencentPassword
import io.ktor.server.routing.*
import java.net.URL
import java.net.http.HttpRequest

object WeChat{
    private const val TARGET_URL =
        "/wechat"
    private const val ENDPOINT =
        "https://open.weixin.qq.com/connect/qrconnect?"
    private const val PARAM =
        "appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login"
    
    fun configureRoute(route: Route) {
        route.post(TARGET_URL){

        }
    }

    fun signin(config: TencentPassword) {
        val req = HttpRequest.newBuilder()
            .uri(URL(ENDPOINT).toURI())
            .GET()

    }
}