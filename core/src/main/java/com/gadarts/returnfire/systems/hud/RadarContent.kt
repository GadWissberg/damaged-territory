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
    private val bitMap: Array<Array<Int>>,
) : Actor() {
    private val playerDot = createDotTexture(Color.BROWN, DOT_SIZE_CHARACTER)
    private val enemyDot = createDotTexture(Color.GREEN, DOT_SIZE_CHARACTER)
    private val signatures by lazy {
        mapOf(
            0b11010000 to assetsManager.getTexture("radar_tile_bottom_right"),
            0b01101000 to assetsManager.getTexture("radar_tile_bottom_left"),
            0b00010110 to assetsManager.getTexture("radar_tile_top_right"),
            0b00001011 to assetsManager.getTexture("radar_tile_top_left"),
            0b11111000 to assetsManager.getTexture("radar_tile_bottom"),
            0b11010110 to assetsManager.getTexture("radar_tile_right"),
            0b01101011 to assetsManager.getTexture("radar_tile_left"),
            0b00011111 to assetsManager.getTexture("radar_tile_top"),
            0b11111110 to assetsManager.getTexture("radar_tile_gulf_bottom_right"),
            0b11111011 to assetsManager.getTexture("radar_tile_gulf_bottom_left"),
            0b11011111 to assetsManager.getTexture("radar_tile_gulf_top_right"),
            0b01111111 to assetsManager.getTexture("radar_tile_gulf_top_left"),
            0b11111111 to assetsManager.getTexture("radar_tile_ground"),
            0 to assetsManager.getTexture("radar_tile_water")
        )
    }

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
                val signature = calculateSignature(tileX, tileZ)
                val dotX = topLeftX + (dx + RADIUS) * cellSize
                val dotY = topLeftY + (RADIUS - dz) * cellSize
                batch.draw(signatures[signature] ?: signatures[0], dotX, dotY, cellSize, cellSize)
            }
        }
        drawCharacters(batch)
    }

    private fun calculateSignature(tileX: Int, tileZ: Int): Int {
        if (!isPositionInMap(tileX, tileZ)) return 0

        val width = tilesMapping.size
        val depth = tilesMapping[0].size - 1
        var signature = 0
        val up = tileZ - 1
        val left = tileX - 1
        val right = tileX + 1
        val down = tileZ + 1
        if (tileX > 0 && tileZ > 0) {
            signature = signature or ((bitMap[up][left]) shl 7)
        }
        if (tileZ > 0) {
            signature = signature or ((bitMap[up][tileX]) shl 6)
        }
        if (tileX < width - 1 && tileZ > 0) {
            signature = signature or ((bitMap[up][right]) shl 5)
        }
        if (tileX > 0) {
            signature = signature or ((bitMap[tileZ][left]) shl 4)
        }
        if (tileX < width - 1) {
            signature = signature or ((bitMap[tileZ][right]) shl 3)
        }
        if (tileX > 0 && tileZ < depth) {
            signature = signature or ((bitMap[down][left]) shl 2)
        }
        if (tileZ < depth) {
            signature = signature or ((bitMap[down][tileX]) shl 1)
        }
        if (tileX < width && tileZ < depth) {
            signature = signature or ((bitMap[down][right]) shl 0)
        }
        return signature
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
