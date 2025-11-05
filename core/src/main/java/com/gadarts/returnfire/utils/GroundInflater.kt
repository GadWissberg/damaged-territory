package com.gadarts.returnfire.utils

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
import com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.AnimatedTextureComponent
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.systems.data.map.InGameTilesLayer
import com.gadarts.returnfire.ecs.systems.data.map.LayerRegion
import com.gadarts.returnfire.ecs.systems.data.map.LayerRegion.Companion.LAYER_REGION_SIZE
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.map.GameMapTileLayer
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo

class GroundInflater(
    private val gameSessionData: GameSessionData,
    private val gamePlayManagers: GamePlayManagers,
    private val engine: Engine
) {
    fun addGround(currentMap: GameMap, exculdedTiles: ArrayList<Pair<Int, Int>>) {
        currentMap.layers.forEachIndexed { index, layer ->
            addFloorLayer(exculdedTiles, layer, index)
        }
        gameSessionData.mapData.tilesEntitiesByLayers.forEach { layer ->
            addFloorModelsToCache(layer)
        }
        addAllExternalSea(currentMap.width, currentMap.depth)
    }

    private fun flatAllTilesBelow(
        index: Int,
        z: Int,
        x: Int,
    ) {
        deleteAllTilesBelow(index, z, x)
        for (i in 0 until index) {
            val mapData = gameSessionData.mapData
            if (i > 0 && mapData.loadedMap.layers[i].tiles[z * mapData.loadedMap.width + x].code == INITIAL_INDEX_OF_TILES_MAPPING) continue

            addTileEntity(mapData.tilesEntitiesByLayers[i].tilesEntities, z, x)
            addTile(Triple(x, calculateLayerHeight(i), z), i, true)
        }
    }

    private fun createAndAddGroundTileEntity(modelInstance: GameModelInstance, position: Vector3): Entity {
        return gamePlayManagers.ecs.entityBuilder.begin()
            .addGroundComponent()
            .addModelInstanceComponent(
                modelInstance, position,
                null,
            )
            .finishAndAddToEngine()
    }
    private fun applyAnimatedTextureComponentToFloor(
        textureDefinition: TextureDefinition,
        entity: Entity
    ) {
        val frames = com.badlogic.gdx.utils.Array<Texture>()
        for (i in 0 until textureDefinition.frames) {
            frames.add(gamePlayManagers.assetsManager.getTexture(textureDefinition, i))
        }
        val animation = Animation(0.25F, frames)
        animation.playMode = Animation.PlayMode.NORMAL
        entity.add(AnimatedTextureComponent(animation))
    }
    private fun addTileEntity(
        tilesEntities: Array<Array<Entity?>>,
        row: Int,
        col: Int
    ) {
        val entityBuilder = gamePlayManagers.ecs.entityBuilder

        val tileEntity = entityBuilder.begin()
            .addGroundComponent()
            .finishAndAddToEngine()
        tilesEntities[row][col] = tileEntity
    }
    private fun calculateLayerHeight(index: Int) = index * 0.0008F
    private fun isTileBumpy(
        x: Int,
        z: Int,
        tilesEntities: Array<Array<Entity?>>,
    ) = (x > 0
        && z > 0
        && x < tilesEntities[0].size
        && z < tilesEntities.size
        && tilesEntities[z][x] != null
        && ComponentsMapper.modelInstance.has(tilesEntities[z][x])
        && ComponentsMapper.modelInstance.get(tilesEntities[z][x]).gameModelInstance.gameModelInstanceInfo?.modelDefinition == ModelDefinition.TILE_BUMPY)
    private fun deleteAllTilesBelow(layerIndex: Int, z: Int, x: Int) {
        for (i in 0 until layerIndex) {
            deleteTile(i, z, x)
        }
    }
    private fun deleteTile(layerIndex: Int, z: Int, x: Int) {
        val inGameTilesLayer = gameSessionData.mapData.tilesEntitiesByLayers[layerIndex]
        engine.removeEntity(
            inGameTilesLayer.tilesEntities[z][x] ?: return
        )
        inGameTilesLayer.tilesEntities[z][x] = null
    }

    private fun initializeExternalSeaTextureAttribute(
        textureAttribute: TextureAttribute,
        width: Int,
        depth: Int
    ) {
        textureAttribute.textureDescription.uWrap = Texture.TextureWrap.Repeat
        textureAttribute.textureDescription.vWrap = Texture.TextureWrap.Repeat
        textureAttribute.offsetU = 0F
        textureAttribute.offsetV = 0F
        textureAttribute.scaleU = width.toFloat()
        textureAttribute.scaleV = depth.toFloat()
    }

    private fun addTile(
        position: Triple<Int, Float, Int>,
        layerIndex: Int,
        forceFlat: Boolean = false
    ) {
        val x = position.first
        val z = position.third
        val tileEntity = gameSessionData.mapData.tilesEntitiesByLayers[layerIndex].tilesEntities[z][x] ?: return

        val assetsManager = gamePlayManagers.assetsManager
        val layer = gameSessionData.mapData.loadedMap.layers[layerIndex]
        val textureDefinition = MapUtils.determineTextureOfMapPosition(
            z,
            x,
            assetsManager.getTexturesDefinitions(),
            layer,
            gameSessionData.mapData.loadedMap
        )
        var randomDirection = false
        val modelDefinition = if (!forceFlat && textureDefinition.bumpyTile && areNeighboringTilesFlat(x, z, layerIndex)
        ) {
            deleteAllTilesBelow(layerIndex, z, x)
            randomDirection = true
            ModelDefinition.TILE_BUMPY
        } else {
            flatAllTilesBelow(layerIndex, z, x)
            ModelDefinition.TILE_FLAT
        }

        val gameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        gameModelInstance.modelInstance.materials[0].set(BlendingAttribute(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1F))
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val realPosition = auxVector1.set(x.toFloat() + 0.5F, position.second, z.toFloat() + 0.5F)
        gameModelInstance.modelInstance.transform.setTranslation(realPosition)
        entityBuilder.addModelInstanceComponentToEntity(
            tileEntity,
            gameModelInstance,
            realPosition,
            null,
        )

        if (randomDirection) {
            gameModelInstance.modelInstance.transform.rotate(Vector3.Y, 90F * MathUtils.random(3))
        }

        applyTextureToFloorTile(x, z, tileEntity, gameModelInstance, layer)
    }

    private fun addFloorModels(
        exculdedTiles: ArrayList<Pair<Int, Int>>,
        height: Float,
        index: Int,
    ) {
        val loadedMap = gameSessionData.mapData.loadedMap
        val width = loadedMap.width
        for (row in 0 until loadedMap.depth) {
            for (col in 0 until width) {
                if (!exculdedTiles.contains(Pair(col, row))) {
                    addTile(Triple(col, height, row), index)
                }
            }
        }
    }

    private fun applyTextureToFloorTile(
        col: Int,
        row: Int,
        entity: Entity,
        modelInstance: GameModelInstance,
        layer: GameMapTileLayer
    ): TextureDefinition? {
        var textureDefinition: TextureDefinition? = null
        val assetsManager = gamePlayManagers.assetsManager
        if (isPositionInsideBoundaries(row, col)) {
            textureDefinition = MapUtils.determineTextureOfMapPosition(
                row,
                col,
                assetsManager.getTexturesDefinitions(),
                layer,
                gameSessionData.mapData.loadedMap
            )
        }
        if (textureDefinition != null) {
            val texture =
                assetsManager.getTexture(textureDefinition)
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            (modelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute)
                .set(TextureRegion(texture))
            if (textureDefinition.animated) {
                applyAnimatedTextureComponentToFloor(textureDefinition, entity)
            }
            if (textureDefinition.fileName.contains("water")) {
                ComponentsMapper.ground.get(entity).isWater = true
            }
        }
        return textureDefinition
    }

    private fun addExtSea(width: Int, depth: Int, x: Float, z: Float) {
        val assetsManager = gamePlayManagers.assetsManager
        val modelInstance = GameModelInstance(
            ModelInstance(ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TILE_BUMPY))),
            ImmutableGameModelInstanceInfo(ModelDefinition.TILE_BUMPY),
        )
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        gamePlayManagers.ecs.entityBuilder.addModelCacheComponentToEntity(entity)
        modelInstance.modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.modelInstance.materials.first()
                .get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        val layerRegion = LayerRegion(x.toInt() - width / 2F, z.toInt() - depth / 2F, width, depth)
        layerRegion.begin()
        layerRegion.add(modelInstance.modelInstance)
        layerRegion.end()
        gameSessionData.mapData.externalSeaRegions.add(layerRegion)
        val texturesDefinitions = assetsManager.getTexturesDefinitions()
        applyAnimatedTextureComponentToFloor(
            texturesDefinitions.definitions["tile_water"]!!,
            entity
        )
    }

    private fun isPositionInsideBoundaries(row: Int, col: Int) = (row >= 0
        && col >= 0
        && row < gameSessionData.mapData.loadedMap.depth
        && col < gameSessionData.mapData.loadedMap.width)

    private fun addFloorLayer(exculdedTiles: ArrayList<Pair<Int, Int>>, layer: GameMapTileLayer, index: Int) {
        val depth = gameSessionData.mapData.loadedMap.depth
        val width = gameSessionData.mapData.loadedMap.width
        val groundBitMap = gameSessionData.mapData.groundBitMap
        val height = calculateLayerHeight(index)

        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val inGameTilesLayer = gameSessionData.mapData.tilesEntitiesByLayers[index]
        val tilesEntities = inGameTilesLayer.tilesEntities
        for (row in 0 until depth) {
            for (col in 0 until width) {
                if (index == 0 || layer.tiles[row * width + col].code != INITIAL_INDEX_OF_TILES_MAPPING) {
                    addTileEntity(tilesEntities, row, col)
                }
            }
        }

        if (index > 0) {
            for (row in 0 until depth) {
                for (col in 0 until width) {
                    val textureDefinition = MapUtils.determineTextureOfMapPosition(
                        row,
                        col,
                        gamePlayManagers.assetsManager.getTexturesDefinitions(),
                        layer,
                        gameSessionData.mapData.loadedMap
                    )
                    if (groundBitMap[row][col] == 0
                        && (!textureDefinition.fileName.contains("water")
                            || exculdedTiles.contains(Pair(col, row)))
                    ) {
                        groundBitMap[row][col] = 1
                    }
                    if (textureDefinition.fileName.contains("road") && !exculdedTiles.contains(
                            Pair(col, row)
                        )
                    ) {
                        entityBuilder.addRoadComponentToEntity(tilesEntities[row][col]!!, textureDefinition)
                        addTile(Triple(col, height, row), index)
                        exculdedTiles.add(Pair(col, row))
                    }

                }
            }
        }
        addFloorModels(exculdedTiles, height, index)
    }

    private fun addFloorModelsToCache(layer: InGameTilesLayer) {
        val layerRegions = layer.layerRegions
        val tilesEntities = layer.tilesEntities

        for (chunkRow in layerRegions.indices) {
            for (chunkCol in layerRegions[chunkRow].indices) {
                var layerRegion = layerRegions[chunkRow][chunkCol]
                val startRow = chunkRow * LAYER_REGION_SIZE
                val startCol = chunkCol * LAYER_REGION_SIZE
                if (layerRegion == null) {
                    layerRegion =
                        LayerRegion(startCol.toFloat(), startRow.toFloat(), LAYER_REGION_SIZE, LAYER_REGION_SIZE)
                    layerRegions[chunkRow][chunkCol] = layerRegion
                }
                layerRegion.begin()
                val endRow = minOf(startRow + LAYER_REGION_SIZE, tilesEntities.size)
                val endCol = minOf(startCol + LAYER_REGION_SIZE, tilesEntities[0].size)
                for (row in startRow until endRow) {
                    for (col in startCol until endCol) {
                        val tileEntity = tilesEntities[row][col]
                        if (tileEntity != null) {
                            val tileModelInstance = ComponentsMapper.modelInstance.get(tileEntity)
                            if (tileModelInstance != null) {
                                layerRegion.add(tileModelInstance.gameModelInstance.modelInstance)
                                gamePlayManagers.ecs.entityBuilder.addModelCacheComponentToEntity(tileEntity)
                            }
                        }
                    }
                }
                layerRegion.end()
            }
        }
    }

    private fun addAllExternalSea(width: Int, depth: Int) {
        addExtSea(width, EXT_SIZE, width / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -EXT_SIZE / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, -EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(width, EXT_SIZE, width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, width + EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, -EXT_SIZE / 2F)
    }

    private fun areNeighboringTilesFlat(x: Int, z: Int, layerIndex: Int): Boolean {
        val mapData = gameSessionData.mapData
        val tilesEntities = mapData.tilesEntitiesByLayers[layerIndex].tilesEntities
        if (isTileBumpy(x - 1, z, tilesEntities)) return false
        if (isTileBumpy(x - 1, z + 1, tilesEntities)) return false
        if (isTileBumpy(x, z + 1, tilesEntities)) return false
        if (isTileBumpy(x + 1, z + 1, tilesEntities)) return false
        if (isTileBumpy(x + 1, z, tilesEntities)) return false
        if (isTileBumpy(x + 1, z - 1, tilesEntities)) return false
        if (isTileBumpy(x, z - 1, tilesEntities)) return false
        if (isTileBumpy(x - 1, z - 1, tilesEntities)) return false

        return true
    }

    companion object {
        private const val EXT_SIZE = 48
        private val auxVector1 = Vector3()
    }
}
