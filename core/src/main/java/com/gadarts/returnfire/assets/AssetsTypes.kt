package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.*
import com.gadarts.returnfire.assets.definitions.model.ModelDefinition

enum class AssetsTypes(
    val format: String,
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    val loadedUsingLoader: Boolean = true,
    val skipAutoLoad: Boolean = false
) {
    TEXTURES("png"),
    SHADERS("glsl", ShaderDefinition.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS("ttf", FontDefinition.entries.toTypedArray()),
    MODELS("g3dj", ModelDefinition.entries.toTypedArray()),
    SFX("wav", SoundDefinition.entries.toTypedArray()),
    MUSIC("ogg", MusicDefinition.entries.toTypedArray()),
    MAPS("json", MapDefinition.entries.toTypedArray(), skipAutoLoad = true),

}
