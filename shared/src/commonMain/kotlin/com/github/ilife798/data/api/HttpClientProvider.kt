package com.github.ilife798.data.api

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
