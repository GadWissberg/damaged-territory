package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.*

enum class AssetsTypes(
    val format: String,
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    val loadedUsingLoader: Boolean = true,
) {
    TEXTURES("png"),
    SHADERS("glsl", ShaderDefinition.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS("ttf", FontsDefinition.entries.toTypedArray()),
    MODELS("g3dj", ModelDefinition.entries.toTypedArray()),
    SFX("wav", SoundDefinition.entries.toTypedArray()),
    MAPS("json", MapDefinition.entries.toTypedArray());

}
