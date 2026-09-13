package com.github.ilife798.data.viewmodel

// 是否为可重试的瞬时网络异常（如 DNS 解析失败、连接/读取超时）。
internal expect fun isTransientNetworkError(e: Throwable): Boolean
