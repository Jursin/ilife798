package com.github.ilife798.data.viewmodel

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorResourceUnavailable
import platform.Foundation.NSURLErrorSecureConnectionFailed
import platform.Foundation.NSURLErrorServerCertificateHasBadDate
import platform.Foundation.NSURLErrorServerCertificateUntrusted
import platform.Foundation.NSURLErrorTimedOut

// 与 Android 分类对齐：DNS/连接失败、超时、断网视为可重试；
// SSL/TLS 错误不重试；未知异常归通用失败。
internal actual fun isTransientNetworkError(e: Throwable): Boolean =
    when (e) {
        is ConnectTimeoutException, is SocketTimeoutException -> true
        is DarwinHttpRequestException -> e.origin.isTransientUrlError()
        else -> e.cause?.let { cause -> cause !== e && isTransientNetworkError(cause) } ?: false
    }

private fun NSError.isTransientUrlError(): Boolean {
    if (domain != NSURLErrorDomain) return false
    return when (code) {
        NSURLErrorTimedOut,
        NSURLErrorCannotFindHost,
        NSURLErrorCannotConnectToHost,
        NSURLErrorDNSLookupFailed,
        NSURLErrorResourceUnavailable,
        NSURLErrorNotConnectedToInternet,
        NSURLErrorNetworkConnectionLost,
        -> true

        NSURLErrorSecureConnectionFailed,
        NSURLErrorServerCertificateHasBadDate,
        NSURLErrorServerCertificateUntrusted,
        -> false

        else -> false
    }
}
