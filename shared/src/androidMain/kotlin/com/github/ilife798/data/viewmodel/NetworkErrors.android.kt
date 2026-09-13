package com.github.ilife798.data.viewmodel

import java.io.IOException
import javax.net.ssl.SSLException

// 把 DNS 解析失败、连接超时等 IOException 视为可重试；SSL 握手/证书错误不重试。
internal actual fun isTransientNetworkError(e: Throwable): Boolean {
    if (e is SSLException) return false
    if (e is IOException) return true
    val cause = e.cause ?: return false
    return cause !== e && isTransientNetworkError(cause)
}
