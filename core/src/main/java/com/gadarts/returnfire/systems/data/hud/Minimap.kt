package com.gadarts.returnfire.systems.data.hud

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.utils.MapUtils

class Minimap(
    private val tilesMapping: Array<CharArray>,
    private val assetsManager: GameAssetManager
) : Actor() {
    private val texturesCache = mutableMapOf<TextureDefinition, Texture>()

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        val cols = tilesMapping[0].size
        val rows = tilesMapping.size

        val scaleX = width / cols
        val scaleY = height / rows

        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val drawX = x * scaleX + getX()
                val drawY = height - (y + 1) * scaleY + getY()
                val textureDefinition =
                    MapUtils.determineTextureOfMapPosition(y, x, assetsManager.getTexturesDefinitions(), tilesMapping)
                texturesCache.getOrPut(textureDefinition) {
                    assetsManager.getTexture(textureDefinition)
                }.let { texture ->
                    batch.draw(texture, drawX, drawY, scaleX, scaleY)
                }
            }
        }
    }
}
