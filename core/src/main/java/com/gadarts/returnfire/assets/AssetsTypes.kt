package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.*

enum class AssetsTypes(
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    val loadedUsingLoader: Boolean = true,
) {
    TEXTURES,
    SHADERS(ShaderDefinition.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS(FontsDefinition.entries.toTypedArray()),
    MODELS(ModelDefinition.entries.toTypedArray()),
    SFX(SoundDefinition.entries.toTypedArray()),
    MAPS(MapDefinition.entries.toTypedArray());

}
