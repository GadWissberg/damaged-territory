package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.utils.ModelUtils

class RadarContentActor(
    private val tilesMapping: Array<CharArray>,
    private val player: Entity,
) : Actor() {
    private val waterDot = createDotTexture(Color.BLUE)
    private val groundDot = createDotTexture(Color.YELLOW)
    private val playerDot = createDotTexture(Color.BROWN, 12)

    private fun createDotTexture(color: Color, size: Int = DOT_SIZE): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fillRectangle(0, 0, size, size)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private val cellSize = RADAR_SIZE / (RADIUS * 2 + 1)

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = auxColor.set(color).also { it.a *= parentAlpha }
        val positionOfModel = ModelUtils.getPositionOfModel(player)
        val topLeftX = x + (width - RADAR_SIZE) / 2
        val topLeftY = y + (height - RADAR_SIZE) / 2
        for (dz in -RADIUS..RADIUS) {
            for (dx in -RADIUS..RADIUS) {
                val tileX = positionOfModel.x.toInt() + dx
                val tileZ = positionOfModel.z.toInt() + dz
                val tile = if (tileX >= 0 && tileX < tilesMapping[0].size && tileZ >= 0 && tileZ < tilesMapping.size) {
                    tilesMapping[tileZ][tileX]
                } else {
                    '0'
                }
                val texture = when (tile) {
                    '0' -> waterDot
                    else -> groundDot
                }
                val dotX = topLeftX + (dx + RADIUS) * cellSize
                val dotY = topLeftY + (RADIUS - dz) * cellSize
                batch.draw(texture, dotX, dotY, cellSize, cellSize)
            }
        }
        val playerDotX = topLeftX + RADIUS * cellSize - DOT_SIZE / 2
        val playerDotY = topLeftY + RADIUS * cellSize - DOT_SIZE / 2
        batch.draw(playerDot, playerDotX, playerDotY)
    }

    companion object {
        private val auxColor = Color()
        private const val RADIUS = 20
        private const val RADAR_SIZE = 192F
        private const val DOT_SIZE = 3
    }
}
