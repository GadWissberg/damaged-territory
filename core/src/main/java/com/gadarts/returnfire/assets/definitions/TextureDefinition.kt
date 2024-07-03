package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.Texture

enum class TextureDefinition(val fileNames: Int = 1, ninepatch: Boolean = false, val animated: Boolean = false) :
    AssetDefinition<Texture> {

    BUTTON_UP,
    BUTTON_DOWN,
    PROPELLER_BLURRED,
    SPARK(3),
    SAND_DEC(4),
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
    BUSH(3),
    TILE_BEACH,
    TILE_BEACH_BOTTOM(fileNames = 2, animated = true),
    TILE_BEACH_BOTTOM_RIGHT(fileNames = 2, animated = true),
    TILE_BEACH_GULF_BOTTOM_RIGHT(fileNames = 2, animated = true),
    TILE_BEACH_BOTTOM_LEFT(fileNames = 2, animated = true),
    TILE_BEACH_GULF_BOTTOM_LEFT(fileNames = 2, animated = true),
    TILE_BEACH_TOP_LEFT(fileNames = 2, animated = true),
    TILE_BEACH_GULF_TOP_LEFT(fileNames = 2, animated = true),
    TILE_BEACH_TOP_RIGHT(fileNames = 2, animated = true),
    TILE_BEACH_GULF_TOP_RIGHT(fileNames = 2, animated = true),
    TILE_BEACH_TOP(fileNames = 2, animated = true),
    TILE_BEACH_RIGHT(fileNames = 2, animated = true),
    TILE_BEACH_LEFT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW(fileNames = 4, animated = true),
    TILE_WATER_SHALLOW_BOTTOM(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_BOTTOM_RIGHT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_GULF_BOTTOM_RIGHT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_BOTTOM_LEFT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_GULF_BOTTOM_LEFT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_TOP_LEFT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_GULF_TOP_LEFT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_TOP_RIGHT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_GULF_TOP_RIGHT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_TOP(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_RIGHT(fileNames = 2, animated = true),
    TILE_WATER_SHALLOW_LEFT(fileNames = 2, animated = true),
    TILE_WATER(fileNames = 4, animated = true),
    JOYSTICK,
    JOYSTICK_CENTER,
    ICON_BULLETS,
    ICON_MISSILES,
    BASE_DOOR;


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
