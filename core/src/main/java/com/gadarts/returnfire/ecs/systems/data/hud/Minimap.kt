package com.gadarts.returnfire.ecs.systems.data.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionDataGameplay
import com.gadarts.returnfire.utils.MapUtils
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.map.GameMap

class Minimap(
    private val gameMap: GameMap,
    private val assetsManager: GameAssetManager,
    private val gamePlayData: GameSessionDataGameplay,
    private val flags: ImmutableArray<Entity>
) : Actor() {
    private val texturesCache = mutableMapOf<TextureDefinition, Texture>()
    private val locationIndicatorTexture by lazy { assetsManager.getTexture("minimap_location") }
    private val flagIndicatorTexture by lazy { assetsManager.getTexture("minimap_flag") }
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
        drawFlags(batch)
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
        val player = gamePlayData.player
        if (player != null) {
            val position: Vector3 = ModelUtils.getPositionOfModel(player)
            val playerX = position.x.toInt()
            val playerY = position.z.toInt()
            drawIcon(playerX, playerY, batch, locationIndicatorTexture, auxColor.set(Color.WHITE))
        }
    }

    private fun drawFlags(batch: Batch) {
        for (flag in flags) {
            val position: Vector3 = ModelUtils.getPositionOfModel(flag)
            val flagX = position.x.toInt()
            val flagY = position.z.toInt()
            val color = ComponentsMapper.flag.get(flag).color
            drawIcon(flagX, flagY, batch, flagIndicatorTexture, auxColor.set(color.color))
        }
    }

    private fun drawIcon(
        iconX: Int,
        iconY: Int,
        batch: Batch,
        texture: Texture,
        color: Color,
    ) {
        if (iconY >= 0 && iconX >= 0 && iconY < gameMap.depth && iconX < gameMap.width) {
            val cols = gameMap.width
            val rows = gameMap.depth
            val scaleX = width / cols
            val scaleY = height / rows
            val drawIconX = iconX * scaleX + x
            val drawIconY = height - (iconY + 1) * scaleY + y
            val alpha = 0.5f + 0.5f * MathUtils.sin(flickerTime * 5f)
            val indicatorSize = texture.width

            batch.setColor(color.r, color.g, color.b, alpha)
            batch.draw(
                texture,
                drawIconX + (scaleX - indicatorSize) / 2f,
                drawIconY + (scaleY - indicatorSize) / 2f,
            )
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    companion object {
        private val auxColor = Color()
    }
}
