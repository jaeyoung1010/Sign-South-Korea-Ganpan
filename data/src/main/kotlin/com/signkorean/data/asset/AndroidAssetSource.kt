package com.signkorean.data.asset

import android.content.res.AssetManager

class AndroidAssetSource(
    private val assets: AssetManager,
) : AssetSource {
    override fun readText(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

    override fun exists(path: String): Boolean =
        runCatching {
            assets.open(path).close()
        }.isSuccess
}
