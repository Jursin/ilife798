package com.github.ilife798.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    val name: String = "",
    val size: Long = 0,
    val digest: String? = null,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
)

@Serializable
data class GithubRelease(
    @SerialName("tag_name")
    val tagName: String = "",
    val assets: List<GithubAsset> = emptyList(),
)
