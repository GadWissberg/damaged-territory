package com.gadarts.returnfire.assets

import com.gadarts.returnfire.assets.definitions.*
import com.gadarts.returnfire.assets.definitions.external.ExternalDefinition
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition

enum class AssetsTypes(
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    val loadedUsingLoader: Boolean = true,
    val definitionsClazz: Class<out ExternalDefinition<*>>? = null,
) {
    TEXTURES(definitionsClazz = TextureDefinition::class.java),
    SHADERS(ShaderDefinition.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS(FontsDefinition.entries.toTypedArray()),
    MODELS(ModelDefinition.entries.toTypedArray()),
    SFX(SoundDefinition.entries.toTypedArray()),
    MAPS(MapDefinition.entries.toTypedArray());

}
