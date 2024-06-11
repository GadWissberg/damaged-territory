package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.*

enum class AssetsTypes(
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    private val loadedUsingLoader: Boolean = true
) {
    TEXTURES(TextureDefinition.entries.toTypedArray()),
    SHADERS(ShaderDefinition.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS(FontsDefinition.entries.toTypedArray()),
    MODELS(ModelDefinition.entries.toTypedArray()),
    SFX(SoundDefinition.entries.toTypedArray()),
    MAPS(MapDefinition.entries.toTypedArray());

    fun isLoadedUsingLoader(): Boolean {
        return loadedUsingLoader
    }

}
