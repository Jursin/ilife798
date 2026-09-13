package com.github.ilife798.update

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateTest {
    @Test
    fun compareVersionsTreatsMissingPartsAsZero() {
        assertEquals(0, AppUpdate.compareVersions("1.2.0", "1.2.0"))
        assertEquals(0, AppUpdate.compareVersions("1.2", "1.2.0"))
        assertEquals(0, AppUpdate.compareVersions("1.2.0", "1.2"))
    }

    @Test
    fun compareVersionsIsNumeric() {
        assertTrue(AppUpdate.compareVersions("1.10.0", "1.9.0") > 0)
        assertTrue(AppUpdate.compareVersions("2.0.0", "10.0.0") < 0)
        assertTrue(AppUpdate.compareVersions("1.0.1", "1.0.0") > 0)
    }

    @Test
    fun selectAssetFollowsAbiPriority() {
        val release = releaseWithAllAbis()
        assertEquals(
            "ilife798-android-arm64-v8a-release.apk",
            AppUpdate.selectAsset(release, listOf("arm64-v8a", "armeabi-v7a"))?.name,
        )
        assertEquals(
            "ilife798-android-armeabi-v7a-release.apk",
            AppUpdate.selectAsset(release, listOf("armeabi-v7a", "arm64-v8a"))?.name,
        )
        assertEquals(
            "ilife798-android-x86_64-release.apk",
            AppUpdate.selectAsset(release, listOf("x86_64"))?.name,
        )
    }

    @Test
    fun selectAssetIsCaseInsensitiveAndNullWhenMissing() {
        val release = releaseWithAllAbis()
        assertEquals(
            "ilife798-android-arm64-v8a-release.apk",
            AppUpdate.selectAsset(release, listOf("ARM64-V8A"))?.name,
        )
        assertNull(AppUpdate.selectAsset(release, listOf("mips")))
    }

    @Test
    fun applyProxyPrefixesOnlyWhenConfigured() {
        val url = "https://github.com/a/b/releases/download/v1/app.apk"
        assertEquals(url, AppUpdate.applyProxy(url, ""))
        assertEquals(url, AppUpdate.applyProxy(url, "   "))
        assertEquals("https://proxy.example.com/$url", AppUpdate.applyProxy(url, "https://proxy.example.com"))
        assertEquals("https://proxy.example.com/$url", AppUpdate.applyProxy(url, "https://proxy.example.com/"))
    }

    @Test
    fun decodesGithubReleaseJson() {
        val json = Json { ignoreUnknownKeys = true }
        val release =
            json.decodeFromString(
                GithubRelease.serializer(),
                """
                {
                  "tag_name": "v1.2.0",
                  "assets": [
                    {
                      "name": "ilife798-android-arm64-v8a-release.apk",
                      "size": 123,
                      "digest": "sha256:abc",
                      "browser_download_url": "https://example.com/app.apk",
                      "extra": true
                    }
                  ]
                }
                """.trimIndent(),
            )
        assertEquals("v1.2.0", release.tagName)
        assertEquals(1, release.assets.size)
        assertEquals("sha256:abc", release.assets.first().digest)
        assertEquals("https://example.com/app.apk", release.assets.first().browserDownloadUrl)
        assertEquals(123L, release.assets.first().size)
    }

    private fun releaseWithAllAbis() =
        GithubRelease(
            assets =
                listOf(
                    GithubAsset(name = "ilife798-android-arm64-v8a-release.apk"),
                    GithubAsset(name = "ilife798-android-armeabi-v7a-release.apk"),
                    GithubAsset(name = "ilife798-android-x86_64-release.apk"),
                ),
        )
}
