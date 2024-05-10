package com.gadarts.returnfire.assets

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.Texture

enum class TexturesDefinitions(fileNames: Int = 1, ninepatch: Boolean = false) :
    AssetDefinition<Texture> {

    JOYSTICK,
    JOYSTICK_CENTER,
    BUTTON_UP,
    BUTTON_DOWN,
    ICON_BULLETS,
    ICON_MISSILES,
    PROPELLER_BLURRED,
    SPARK(3),
    SAND,
    SAND_DEC(4),
    VERTICAL,
    HORIZONTAL,
    CROSS(2),
    HORIZONTAL_BOTTOM(2),
    HORIZONTAL_TOP(2),
    VERTICAL_LEFT(2),
    VERTICAL_RIGHT(2),
    LEFT_TO_BOTTOM(2),
    RIGHT_TO_BOTTOM(2),
    LEFT_TO_TOP(2),
    RIGHT_TO_TOP(2),
    RIGHT_END(2),
    LEFT_END(2),
    TOP_END(2),
    BOTTOM_END(2),
    BUSH(3);

    private val paths = ArrayList<String>()

    init {
        initializePaths("textures/${(if (ninepatch) "%s.9" else "%s")}.png", fileNames)
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<Texture>? {
        return null
    }

    override fun getClazz(): Class<Texture> {
        return Texture::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
