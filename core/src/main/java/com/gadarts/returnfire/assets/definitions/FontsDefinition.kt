package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.graphics.g2d.BitmapFont

enum class FontsDefinition : AssetDefinition<BitmapFont> {
    ;


    override fun getClazz(): Class<BitmapFont> {
        return BitmapFont::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
