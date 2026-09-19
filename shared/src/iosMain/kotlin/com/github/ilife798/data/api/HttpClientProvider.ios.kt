package com.github.ilife798.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// 与 Android OkHttp 实现保持同等语义：同一 JSON 配置与默认请求头；引擎为 iOS 原生 Darwin。
// 超时差异：Android 侧依赖 OkHttp 引擎默认超时；Darwin 引擎使用系统 NSURLSession 默认
// （请求级 60s），不支持独立的 connect/socket 超时设置。
actual fun createHttpClient(): HttpClient =
    HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            header("Accept-Language", "zh-Hans-CN;q=1")
            header("User-Agent", ApiConfig.USER_AGENT)
            header("VersionCode", ApiConfig.VERSION_CODE)
        }
    }
