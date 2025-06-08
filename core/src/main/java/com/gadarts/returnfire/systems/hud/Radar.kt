package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class Radar(
    private val player: Entity,
    private val enemies: ImmutableArray<Entity>,
    private val assetsManager: GameAssetManager,
    private val bitMap: Array<Array<Int>>,
) : Actor(), Disposable {
    private var radarTileMap: Array<Array<Int>>
    private val playerDot by lazy { assetsManager.getTexture("radar_character_brown") }
    private val enemyDot by lazy { assetsManager.getTexture("radar_character_green") }
    private val dashLine by lazy { TextureRegion(assetsManager.getTexture("radar_dash_line")) }
    private val signatures by lazy {
        mapOf(
            0 to assetsManager.getTexture("radar_tile_water"),
            0b00000010 to assetsManager.getTexture("radar_tile_top"),
            0b01000000 to assetsManager.getTexture("radar_tile_bottom"),
            0b00001000 to assetsManager.getTexture("radar_tile_left"),
            0b00010000 to assetsManager.getTexture("radar_tile_right"),
            0b00010110 to assetsManager.getTexture("radar_tile_gulf_top_right"),
            0b00001011 to assetsManager.getTexture("radar_tile_gulf_top_left"),
            0b11010000 to assetsManager.getTexture("radar_tile_gulf_bottom_right"),
            0b01101000 to assetsManager.getTexture("radar_tile_gulf_bottom_left"),
            0b10000000 to assetsManager.getTexture("radar_tile_bottom_right"),
            0b00100000 to assetsManager.getTexture("radar_tile_bottom_left"),
            0b00000100 to assetsManager.getTexture("radar_tile_top_right"),
            0b00000001 to assetsManager.getTexture("radar_tile_top_left"),
            0b11111111 to assetsManager.getTexture("radar_tile_ground"),
        )
    }

    init {
        val width = bitMap[0].size
        radarTileMap = Array(bitMap.size) { y ->
            Array(width) { x ->
                val signature = SharedUtils.calculateTileSignature(x, y, bitMap)
                val textureSignature = signatures.keys
                    .sortedByDescending { it.countOneBits() }
                    .find { (it and signature) == it }
                textureSignature ?: 0
            }
        }
    }

    private val scanlines = createScanlineTexture()
    private var sweepTime = 0f

    private val cellSize = RADAR_SIZE / (RADIUS * 2 + 1)
    private var scanlineOffset = 0f
    private var scanlineTime = 0f
    private val sweepTexture = createSweepTexture()

    private fun createSweepTexture(): Texture {
        val width = RADAR_SIZE.toInt()
        val height = 20
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)

        for (y in 0 until height) {
            val alpha = y / height.toFloat()
            pixmap.setColor(0f, 1f, 0f, alpha * 0.3f)
            pixmap.drawLine(0, y, width, y)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun createScanlineTexture(): Texture {
        val width = RADAR_SIZE.toInt()
        val height = 8

        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        pixmap.setColor(0f, 0f, 0f, 0.3f)
        for (y in 0 until height step 4) {
            pixmap.drawLine(0, y, width, y)
        }

        val texture = Texture(pixmap)
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        pixmap.dispose()

        return texture
    }

    override fun act(delta: Float) {
        super.act(delta)
        scanlineOffset = (scanlineOffset + 15 * delta) % scanlines.height
        scanlineTime += delta
        sweepTime += delta
        if (sweepTime > SWEEP_TOTAL_DURATION) {
            sweepTime = 0f
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        batch.color = auxColor.set(color).also { it.a *= parentAlpha }
        val positionOfModel = ModelUtils.getPositionOfModel(player)
        val topLeftX = x + (width - RADAR_SIZE) / 2
        val topLeftY = y + (height - RADAR_SIZE) / 2
        for (dz in -RADIUS..RADIUS) {
            for (dx in -RADIUS..RADIUS) {
                val tileX = positionOfModel.x.toInt() + dx
                val tileZ = positionOfModel.z.toInt() + dz
                val dotX = topLeftX + (dx + RADIUS) * cellSize
                val dotY = topLeftY + (RADIUS - dz) * cellSize
                val clampedZ = MathUtils.clamp(tileZ, 0, bitMap.size - 1)
                val clampedX = MathUtils.clamp(tileX, 0, bitMap[0].size - 1)
                val signature = radarTileMap[clampedZ][clampedX]
                batch.draw(signatures[signature], dotX, dotY, cellSize, cellSize)
            }
        }
        drawCharacters(batch)
        drawDashLines(batch)
        drawScanlines(batch, topLeftX, topLeftY)
        batch.setColor(1f, 1f, 1f, 1F)
    }

    private fun drawScanlines(batch: Batch, topLeftX: Float, topLeftY: Float) {
        val progress = MathUtils.sin(scanlineTime * MathUtils.PI) * 0.5f + 0.5f
        val flickerAlpha = MathUtils.lerp(0.1f, 0.5f, progress)
        batch.setColor(1f, 1f, 1f, flickerAlpha)
        batch.draw(
            scanlines,
            topLeftX, topLeftY,
            RADAR_SIZE, RADAR_SIZE,
            0f, scanlineOffset / scanlines.height,
            RADAR_SIZE / scanlines.width, (scanlineOffset + RADAR_SIZE) / scanlines.height
        )
        val sweepProgress = sweepTime / SWEEP_DURATION
        val sweepY = topLeftY + RADAR_SIZE * (1f - sweepProgress)
        if (sweepY + sweepTexture.height > topLeftY) {
            batch.draw(
                sweepTexture,
                topLeftX,
                sweepY,
                RADAR_SIZE,
                sweepTexture.height.toFloat()
            )
        }
    }

    private fun drawDashLines(batch: Batch) {
        batch.draw(dashLine, x + width / 2F, y)
        batch.draw(dashLine, x + width / 4F, y)
        batch.draw(dashLine, x + 3 * width / 4F, y)
        drawVerticalDashLine(batch, y)
        drawVerticalDashLine(batch, y - height / 4F)
        drawVerticalDashLine(batch, y + height / 4F)
    }

    private fun drawVerticalDashLine(batch: Batch, y: Float) {
        batch.draw(
            dashLine,
            x + width / 2F,
            y,
            dashLine.regionWidth / 2F,
            dashLine.regionHeight / 2F,
            dashLine.regionWidth.toFloat(),
            dashLine.regionHeight.toFloat(),
            1F,
            1F,
            90F
        )
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

    companion object {
        private val auxColor = Color()
        private const val RADIUS = 20
        private const val RADAR_SIZE = 192F
        private const val DOT_SIZE = 3
        private const val DOT_SIZE_CHARACTER = 6
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val SWEEP_DURATION = 4f
        private const val SWEEP_TAIL_PIXELS = 20f
        private const val SWEEP_TOTAL_DURATION = SWEEP_DURATION + (SWEEP_TAIL_PIXELS / RADAR_SIZE) * SWEEP_DURATION
    }

    override fun dispose() {
        sweepTexture.dispose()
        scanlines.dispose()
    }
}
