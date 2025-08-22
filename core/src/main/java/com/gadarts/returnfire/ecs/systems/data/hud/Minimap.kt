package com.gadarts.returnfire.ecs.systems.data.hud

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.ecs.systems.data.GameSessionDataGameplay
import com.gadarts.returnfire.utils.MapUtils
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.map.GameMap

class Minimap(
    private val gameMap: GameMap,
    private val assetsManager: GameAssetManager,
    private val gamePlayData: GameSessionDataGameplay
) : Actor() {
    private val texturesCache = mutableMapOf<TextureDefinition, Texture>()
    private val locationIndicatorTexture by lazy { assetsManager.getTexture("minimap_location") }
    private var flickerTime = 0f
    override fun act(delta: Float) {
        super.act(delta)
        flickerTime += delta
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        for (layerIndex in gameMap.layers.indices) {
            drawLayer(batch, layerIndex)
        }

        drawPlayer(batch)
    }

    private fun drawLayer(
        batch: Batch,
        layerIndex: Int
    ) {
        val cols = gameMap.width
        val rows = gameMap.depth
        val scaleX = width / cols
        val scaleY = height / rows
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val drawX = x * scaleX + getX()
                val drawY = height - (y + 1) * scaleY + getY()
                val gameMapTileLayer = gameMap.layers[layerIndex]
                val textureDefinition =
                    MapUtils.determineTextureOfMapPosition(
                        y,
                        x,
                        assetsManager.getTexturesDefinitions(),
                        gameMapTileLayer,
                        gameMap
                    )
                if (layerIndex == 0 || gameMapTileLayer.tiles[gameMap.width * y + x].code != INITIAL_INDEX_OF_TILES_MAPPING) {
                    texturesCache.getOrPut(textureDefinition) {
                        assetsManager.getTexture(textureDefinition)
                    }.let { texture ->
                        batch.draw(texture, drawX, drawY, scaleX, scaleY)
                    }
                }
            }
        }
    }

    private fun drawPlayer(batch: Batch) {
        val cols = gameMap.width
        val rows = gameMap.depth
        val scaleX = width / cols
        val scaleY = height / rows
        val player = gamePlayData.player
        if (player != null) {
            val position: Vector3 = ModelUtils.getPositionOfModel(player)
            val playerX = position.x.toInt()
            val playerY = position.z.toInt()
            if (playerY >= 0 && playerX >= 0 && playerY < gameMap.depth && playerX < gameMap.width) {
                val drawPlayerX = playerX * scaleX + x
                val drawPlayerY = height - (playerY + 1) * scaleY + y
                val alpha = 0.5f + 0.5f * MathUtils.sin(flickerTime * 5f)
                val indicatorSize = locationIndicatorTexture.width

                batch.setColor(1f, 1f, 1f, alpha)
                batch.draw(
                    locationIndicatorTexture,
                    drawPlayerX + (scaleX - indicatorSize) / 2f,
                    drawPlayerY + (scaleY - indicatorSize) / 2f,
                )
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }
}
