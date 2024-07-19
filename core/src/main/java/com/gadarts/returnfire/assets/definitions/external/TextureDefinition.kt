package com.gadarts.returnfire.assets.definitions.external

import com.badlogic.gdx.graphics.Texture

class TextureDefinition(override val fileName: String, val frames: Int, val animated: Boolean) :
    ExternalDefinition<Texture>
