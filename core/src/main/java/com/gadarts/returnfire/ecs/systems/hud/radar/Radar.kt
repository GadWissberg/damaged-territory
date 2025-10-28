package com.gadarts.returnfire.ecs.systems.hud.radar

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionDataGameplay
import com.gadarts.returnfire.ecs.systems.hud.radar.RadarC.RADAR_SIZE
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class Radar(
    private val gameSessionDataGameplay: GameSessionDataGameplay,
    private val enemies: ImmutableArray<Entity>,
    private val assetsManager: GameAssetManager,
    private val bitMap: Array<Array<Int>>,
) : Actor(), Disposable {
    private val radarTileMap: Array<Array<Int>>
    private val radarRelatedTextures by lazy {
        RadarRelatedTextures(
            assetsManager
        )
    }
    private var sweepTime = 0f
    private val cellSize = RADAR_SIZE / (RADIUS * 2 + 1)
    private var scanlineOffset = 0f
    private var scanlineTime = 0f

    init {
        val width = bitMap[0].size
        radarTileMap = Array(bitMap.size) { y ->
            Array(width) { x ->
                val signature = SharedUtils.calculateTileSignature(x, y, bitMap)
                val textureSignature = radarRelatedTextures.signatures.keys
                    .sortedByDescending { it.countOneBits() }
                    .find { (it and signature) == it }
                textureSignature ?: 0
            }
        }
    }


    override fun act(delta: Float) {
        super.act(delta)
        scanlineOffset = (scanlineOffset + 15 * delta) % radarRelatedTextures.scanLines.height
        scanlineTime += delta
        sweepTime += delta
        if (sweepTime > SWEEP_TOTAL_DURATION) {
            sweepTime = 0f
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val player = gameSessionDataGameplay.player ?: return

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
                val signature =
                    if (tileZ < 0 || tileZ > bitMap.size - 1 || tileX < 0 || tileX > bitMap[0].size - 1) {
                        0
                    } else {
                        radarTileMap[tileZ][tileX]
                    }
                batch.draw(radarRelatedTextures.signatures[signature], dotX, dotY, cellSize, cellSize)
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
        val scanLines = radarRelatedTextures.scanLines
        batch.draw(
            scanLines,
            topLeftX,
            topLeftY,
            RADAR_SIZE,
            RADAR_SIZE,
            0f,
            scanlineOffset / scanLines.height,
            RADAR_SIZE / scanLines.width,
            (scanlineOffset + RADAR_SIZE) / scanLines.height
        )
        val sweepProgress = sweepTime / SWEEP_DURATION
        val sweepY = topLeftY + RADAR_SIZE * (1f - sweepProgress)
        if (sweepY + radarRelatedTextures.sweepTexture.height > topLeftY) {
            batch.draw(
                radarRelatedTextures.sweepTexture,
                topLeftX,
                sweepY,
                RADAR_SIZE,
                radarRelatedTextures.sweepTexture.height.toFloat()
            )
        }
    }

    private fun drawDashLines(batch: Batch) {
        batch.draw(radarRelatedTextures.dashLine, x + width / 2F, y)
        batch.draw(radarRelatedTextures.dashLine, x + width / 4F, y)
        batch.draw(radarRelatedTextures.dashLine, x + 3 * width / 4F, y)
        drawVerticalDashLine(batch, y)
        drawVerticalDashLine(batch, y - height / 4F)
        drawVerticalDashLine(batch, y + height / 4F)
    }

    private fun drawVerticalDashLine(batch: Batch, y: Float) {
        val dashLine = radarRelatedTextures.dashLine
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
        val player = gameSessionDataGameplay.player ?: return

        val positionOfModel = ModelUtils.getPositionOfModel(
            player,
            auxVector1
        )
        val topLeftX = x + (width - RADAR_SIZE) / 2
        val topLeftY = y + (height - RADAR_SIZE) / 2
        val playerDotX = topLeftX + RADIUS * cellSize - DOT_SIZE / 2
        val playerDotY = topLeftY + RADIUS * cellSize - DOT_SIZE / 2
        batch.draw(radarRelatedTextures.playerDot, playerDotX, playerDotY)
        for (i in 0 until enemies.size()) {
            val enemy = enemies[i]
            val enemyPos = ModelUtils.getPositionOfModel(
                enemy,
                auxVector2
            )
            val dx = enemyPos.x.toInt() - positionOfModel.x.toInt()
            val dz = enemyPos.z.toInt() - positionOfModel.z.toInt()
            if (dx in -RADIUS..RADIUS && dz in -RADIUS..RADIUS) {
                val dotX = topLeftX + ((dx + RADIUS) * cellSize)
                val dotY = topLeftY + ((RADIUS - dz) * cellSize)
                batch.draw(
                    radarRelatedTextures.enemyDot,
                    dotX - DOT_SIZE_CHARACTER / 2f,
                    dotY - DOT_SIZE_CHARACTER / 2f
                )
            }
        }
    }

    companion object {
        private val auxColor = Color()
        private const val RADIUS = 20
        private const val DOT_SIZE = 3
        private const val DOT_SIZE_CHARACTER = 6
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val SWEEP_DURATION = 4f
        private const val SWEEP_TAIL_PIXELS = 20f
        private const val SWEEP_TOTAL_DURATION = SWEEP_DURATION + (SWEEP_TAIL_PIXELS / RADAR_SIZE) * SWEEP_DURATION
    }

    override fun dispose() {
        radarRelatedTextures.sweepTexture.dispose()
        radarRelatedTextures.dispose()
    }
}
