package com.signkorean.data.asset

import java.io.FileNotFoundException
import java.security.MessageDigest

internal const val MANIFEST_PATH = "content/manifest.json"
internal const val BASE_CARDS_PATH = "content/cards.base.json"
internal const val EN_CARDS_PATH = "content/en/cards.json"
internal const val JA_CARDS_PATH = "content/ja/cards.json"
internal const val ZH_CARDS_PATH = "content/zh/cards.json"

internal class FakeAssetSource(
    private val assets: Map<String, String>,
) : AssetSource {
    override fun readText(path: String): String = assets[path] ?: throw FileNotFoundException(path)

    override fun exists(path: String): Boolean = path in assets
}

internal class ClasspathAssetSource(
    private val classLoader: ClassLoader = checkNotNull(BundleContentAdapterTest::class.java.classLoader),
) : AssetSource {
    override fun readText(path: String): String =
        classLoader.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: throw FileNotFoundException(path)

    override fun exists(path: String): Boolean = classLoader.getResource(path) != null
}

internal fun validAssets(): Map<String, String> {
    val classLoader = checkNotNull(BundleContentAdapterTest::class.java.classLoader)
    val contentPaths = listOf(MANIFEST_PATH, BASE_CARDS_PATH, EN_CARDS_PATH, JA_CARDS_PATH, ZH_CARDS_PATH)
    val baseCardImagePaths =
        Regex("\\\"imageAsset\\\"\\s*:\\s*\\\"assets/([^\\\"]+)\\\"")
            .findAll(classLoader.getResource(BASE_CARDS_PATH)?.readText().orEmpty())
            .map { it.groupValues[1] }
            .toList()
    val baseCardAudioPaths =
        Regex("\\\"typecastAudioAsset\\\"\\s*:\\s*\\\"assets/([^\\\"]+)\\\"")
            .findAll(classLoader.getResource(BASE_CARDS_PATH)?.readText().orEmpty())
            .map { it.groupValues[1] }
            .toList()
    val binaryPaths =
        (1..20).flatMap { order ->
            val slug = cardSlugs.getValue(order)
            listOf("signs/generated/$slug.jpg", "audio/eunkyung/$slug.mp3")
        } + baseCardImagePaths + baseCardAudioPaths + legacySignImagePaths
    return buildMap {
        contentPaths.forEach { path ->
            put(path, classLoader.getResource(path)?.readText() ?: error("Missing test resource: $path"))
        }
        binaryPaths.forEach { path ->
            if (classLoader.getResource(path) != null) put(path, "binary fixture")
        }
    }
}

internal fun refreshManifestChecksums(assets: MutableMap<String, String>) {
    var manifest = assets.getValue(MANIFEST_PATH)
    assets.keys
        .filter { it.startsWith("content/") && it.endsWith(".json") && it != MANIFEST_PATH }
        .sorted()
        .forEach { path ->
            val checksum = sha256ForTest(assets.getValue(path))
            val checksumPattern = "\"$path\": \"[0-9a-f]{64}\"".toRegex()
            manifest = manifest.replace(checksumPattern, "\"$path\": \"$checksum\"")
        }
    assets[MANIFEST_PATH] = manifest
}

private fun sha256ForTest(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { "%02x".format(it) }

private val cardSlugs =
    mapOf(
        1 to "01-hongdae-entrance",
        2 to "02-gyeongbokgung",
        3 to "03-bukchon-hanok-village",
        4 to "04-namsan-seoul-tower",
        5 to "05-myeongdong",
        6 to "06-gwangjang-market",
        7 to "07-namdaemun-market",
        8 to "08-itaewon",
        9 to "09-hangang-park",
        10 to "10-seongsu-dong",
        11 to "11-insadong",
        12 to "12-ikseon-dong",
        13 to "13-dongdaemun-design-plaza",
        14 to "14-haeundae",
        15 to "15-gamcheon-culture-village",
        16 to "16-jeonju-hanok-village",
        17 to "17-jeju-olle-trail",
        18 to "18-bulguksa",
        19 to "19-busan-station",
        20 to "20-seoul-station",
    )

private val legacySignImagePaths =
    listOf(
        "signs/bunsik.jpg",
        "signs/exit.jpg",
        "signs/pharmacy.jpg",
        "signs/no-parking.jpg",
        "signs/restroom.jpg",
        "signs/convenience-store.jpg",
        "signs/entrance.jpg",
        "signs/open.jpg",
        "signs/no-smoking.jpg",
        "signs/sale.jpg",
    )
