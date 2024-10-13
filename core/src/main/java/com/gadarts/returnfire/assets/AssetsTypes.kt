package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.AssetDefinition
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ShaderDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition

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
    MAPS("json", MapDefinition.entries.toTypedArray(), skipAutoLoad = true),

}
