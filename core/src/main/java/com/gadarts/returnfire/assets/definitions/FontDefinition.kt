package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader

enum class FontDefinition(private val size: Int) :
    AssetDefinition<BitmapFont> {
    WOK_STENCIL(32),
    CONSOLA(15);

    private val paths = ArrayList<String>()

    init {
        initializePaths("fonts/%s.ttf", output = getPaths())
    }

    override fun getParameters(): AssetLoaderParameters<BitmapFont>? {
        return createFontParameters()
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getClazz(): Class<BitmapFont> {
        return BitmapFont::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }

    private fun createFontParameters(
    ): AssetLoaderParameters<BitmapFont> {
        val params = FreetypeFontLoader.FreeTypeFontLoaderParameter()
        params.fontFileName = paths.first()
        params.fontParameters.size = size
        params.fontParameters.color = Color.WHITE
        params.fontParameters.borderColor = Color(0f, 0F, 0f, 0.5f)
        params.fontParameters.borderWidth = 1F
        params.fontParameters.shadowColor = Color(0f, 0F, 0f, 0.5f)
        params.fontParameters.borderStraight = true
        params.fontParameters.kerning = true
        return params
    }
}
