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
    fun selectIpaAssetMatchesPlatformPackage() {
        val release =
            GithubRelease(
                assets =
                    listOf(
                        GithubAsset(
                            name = "ilife798-android-arm64-v8a-release.apk",
                            browserDownloadUrl = "https://example.com/app.apk",
                        ),
                        GithubAsset(
                            name = "ILIFE798-IOS-UNSIGNED.IPA",
                            browserDownloadUrl = "https://example.com/app.ipa",
                        ),
                    ),
            )
        assertEquals("ILIFE798-IOS-UNSIGNED.IPA", AppUpdate.selectIpaAsset(release)?.name)
        assertNull(
            AppUpdate.selectIpaAsset(
                GithubRelease(assets = listOf(GithubAsset(name = "app.apk", browserDownloadUrl = "https://example.com/a.apk"))),
            ),
        )
        assertNull(AppUpdate.selectIpaAsset(GithubRelease(assets = listOf(GithubAsset(name = "a.ipa")))))
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
                    GithubAsset(
                        name = "ilife798-android-arm64-v8a-release.apk",
                        browserDownloadUrl = "https://example.com/arm64-v8a.apk",
                    ),
                    GithubAsset(
                        name = "ilife798-android-armeabi-v7a-release.apk",
                        browserDownloadUrl = "https://example.com/armeabi-v7a.apk",
                    ),
                    GithubAsset(
                        name = "ilife798-android-x86_64-release.apk",
                        browserDownloadUrl = "https://example.com/x86_64.apk",
                    ),
                ),
        )
}
