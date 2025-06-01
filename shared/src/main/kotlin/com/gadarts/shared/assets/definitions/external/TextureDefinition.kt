package com.gadarts.shared.assets.definitions.external

import com.badlogic.gdx.graphics.Texture
import com.gadarts.shared.assets.AssetsTypes

class TextureDefinition(
    override val fileName: String,
    val frames: Int,
    val animated: Boolean,
    val folder: String
) :
    ExternalDefinition<Texture> {
    companion object {
        const val FOLDER: String = "textures"
        const val FORMAT: String = "png"
    }

    override val type: AssetsTypes = AssetsTypes.TEXTURES
}
