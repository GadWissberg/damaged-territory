package com.gadarts.returnfire.systems.data.hud

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.managers.GameAssetManager

class Minimap(
    private val tilesMapping: Array<CharArray>,
    private val assetsManager: GameAssetManager
) : Actor() {
    private val waterTile by lazy {
        assetsManager.getTexture("tile_water")
    }

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

                batch.draw(waterTile, drawX, drawY, scaleX, scaleY)
            }
        }
    }
}