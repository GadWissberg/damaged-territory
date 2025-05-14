package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.utils.ModelUtils

class RadarContent(
    private val tilesMapping: Array<CharArray>,
    private val player: Entity,
    private val enemies: ImmutableArray<Entity>,
    private val assetsManager: GameAssetManager,
) : Actor() {
    private val tileWater: Texture by lazy { assetsManager.getTexture("radar_tile_water") }
    private val tileBeachBottom: Texture by lazy { assetsManager.getTexture("radar_tile_beach_bottom") }
    private val groundDot = createDotTexture(Color.YELLOW)
    private val playerDot = createDotTexture(Color.BROWN, DOT_SIZE_CHARACTER)
    private val enemyDot = createDotTexture(Color.GREEN, DOT_SIZE_CHARACTER)

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
                val tile = if (isPositionInMap(tileX, tileZ)) {
                    tilesMapping[tileZ][tileX]
                } else {
                    '0'
                }
                val texture = when (tile) {
                    '0' -> tileWater
                    else -> groundDot
                }
                val dotX = topLeftX + (dx + RADIUS) * cellSize
                val dotY = topLeftY + (RADIUS - dz) * cellSize
                batch.draw(texture, dotX, dotY, cellSize, cellSize)
            }
        }
        drawCharacters(batch)
    }

    private fun drawCharacters(
        batch: Batch,
    ) {
        val positionOfModel = ModelUtils.getPositionOfModel(player, auxVector1)
        val topLeftX = x + (width - RADAR_SIZE) / 2
        val topLeftY = y + (height - RADAR_SIZE) / 2
        val playerDotX = topLeftX + RADIUS * cellSize - DOT_SIZE / 2
        val playerDotY = topLeftY + RADIUS * cellSize - DOT_SIZE / 2
        batch.draw(playerDot, playerDotX, playerDotY)
        for (i in 0 until enemies.size()) {
            val enemy = enemies[i]
            val enemyPos = ModelUtils.getPositionOfModel(enemy, auxVector2)
            val dx = enemyPos.x.toInt() - positionOfModel.x.toInt()
            val dz = enemyPos.z.toInt() - positionOfModel.z.toInt()
            if (dx in -RADIUS..RADIUS && dz in -RADIUS..RADIUS) {
                val dotX = topLeftX + ((dx + RADIUS) * cellSize)
                val dotY = topLeftY + ((RADIUS - dz) * cellSize)
                batch.draw(enemyDot, dotX - DOT_SIZE_CHARACTER / 2f, dotY - DOT_SIZE_CHARACTER / 2f)
            }
        }
    }

    private fun isPositionInMap(tileX: Int, tileZ: Int) =
        tileX >= 0 && tileX < tilesMapping[0].size && tileZ >= 0 && tileZ < tilesMapping.size

    companion object {
        private val auxColor = Color()
        private const val RADIUS = 20
        private const val RADAR_SIZE = 192F
        private const val DOT_SIZE = 3
        private const val DOT_SIZE_CHARACTER = 6
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
    }
}
