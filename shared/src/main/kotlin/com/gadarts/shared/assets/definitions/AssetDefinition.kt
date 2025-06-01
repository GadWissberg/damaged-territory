package com.gadarts.shared.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import java.util.*

interface AssetDefinition<T> {
    fun getPaths(): ArrayList<String>
    fun getDefinitionName(): String
    fun getClazz(): Class<T>
    fun getParameters(): AssetLoaderParameters<T>?
    fun initializePaths(pathFormat: String, output: ArrayList<String>, fileNames: Int = 1, suffix: String? = null) {
        val definitionName = getDefinitionName()
        if (fileNames == 1) {
            output.add(pathFormat.format(definitionName.lowercase(Locale.ROOT) + (suffix ?: "")))
        } else {
            for (i in 0 until fileNames) {
                output.add(pathFormat.format(definitionName.lowercase(Locale.ROOT) + "_" + i + (suffix ?: "")))
            }
        }
    }

}
