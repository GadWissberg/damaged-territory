package com.gadarts.returnfire.assets

enum class AssetsTypes(
    val assets: Array<out AssetDefinition<*>> = arrayOf(),
    private val loadedUsingLoader: Boolean = true
) {
    TEXTURES(TexturesDefinitions.entries.toTypedArray()),
    SHADERS(ShaderDefinitions.entries.toTypedArray(), loadedUsingLoader = false),
    FONTS(FontsDefinitions.entries.toTypedArray()),
    MODELS(ModelDefinition.entries.toTypedArray()),
    SFX(SfxDefinitions.entries.toTypedArray()),
    MAPS;

    fun isLoadedUsingLoader(): Boolean {
        return loadedUsingLoader
    }

}
