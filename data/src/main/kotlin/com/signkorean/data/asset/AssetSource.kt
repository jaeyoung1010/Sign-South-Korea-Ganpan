package com.signkorean.data.asset

interface AssetSource {
    fun readText(path: String): String

    fun exists(path: String): Boolean
}
