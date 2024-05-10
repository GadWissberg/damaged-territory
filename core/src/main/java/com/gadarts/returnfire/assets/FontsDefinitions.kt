package com.gadarts.returnfire.assets

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader
import java.util.*

enum class FontsDefinitions : AssetDefinition<BitmapFont> {
    ;

    private var path: String = "${name.toLowerCase(Locale.ROOT)}.ttf"

    protected fun createFontParameters(
        size: Int,
        borderWidth: Float
    ): AssetLoaderParameters<BitmapFont> {
        val params = FreetypeFontLoader.FreeTypeFontLoaderParameter()
        params.fontFileName = "varela.ttf"
        params.fontParameters.size = size
        params.fontParameters.color = Color.WHITE
        params.fontParameters.borderColor = Color(0f, 0F, 0f, 0.5f)
        params.fontParameters.borderWidth = borderWidth
        params.fontParameters.shadowColor = Color(0f, 0F, 0f, 0.5f)
        params.fontParameters.shadowOffsetX = -4
        params.fontParameters.shadowOffsetY = 4
        params.fontParameters.borderStraight = true
        params.fontParameters.kerning = true
        return params
    }

    override fun getClazz(): Class<BitmapFont> {
        return BitmapFont::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
