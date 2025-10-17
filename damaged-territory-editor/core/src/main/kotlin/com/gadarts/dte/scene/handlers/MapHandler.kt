package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.dte.*
import com.gadarts.dte.scene.SceneRenderer.Companion.MAP_SIZE
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.scene.handlers.render.EditorModelInstanceProps
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.SharedUtils.INITIAL_INDEX_OF_TILES_MAPPING
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.assets.map.GameMapTileLayer
import com.gadarts.shared.assets.map.TilesMapping
import com.gadarts.shared.data.type.ElementType

class MapHandler(
    private val sharedData: SharedData,
    private val assetsManager: GameAssetManager,
    private val tileFactory: TileFactory,
    private val objectFactory: ObjectFactory,
    messageDispatcher: MessageDispatcher,
) : SceneHandler(messageDispatcher) {
    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        SharedUtils.createFlatMesh(builder, "floor", 0.5F, null, 0F)
        return builder.end()
    }

    override val subscribedEvents: Map<EditorEvents, EditorOnEvent> = mapOf(
        EditorEvents.LAYER_ADDED to object : EditorOnEvent {
            override fun react(msg: com.badlogic.gdx.ai.msg.Telegram) {
                addNewLayer(msg.extraInfo as String, null)
            }
        },
        EditorEvents.MAP_NEW to object : EditorOnEvent {
            override fun react(msg: com.badlogic.gdx.ai.msg.Telegram) {
                clearMapData()
                addNewLayer(DEFAULT_LAYER_NAME, null)
            }
        },
        EditorEvents.MAP_LOADED to object : EditorOnEvent {
            override fun react(msg: com.badlogic.gdx.ai.msg.Telegram) {
                clearMapData()
                val gameMap = msg.extraInfo as GameMap
                inflateLayers(gameMap)
                val definitions = ElementType.entries.flatMap { it.definitions }
                gameMap.objects.map { obj ->
                    objectFactory.addObject(
                        obj.column, obj.row,
                        definitions.first { it.getName().lowercase() == obj.definition.lowercase() },
                        Quaternion(Vector3.Y, obj.rotation ?: 0f)
                    )
                }
            }
        }
    )

    init {
        sharedData.floorModel = createFloorModel()
    }

    private fun inflateLayers(gameMap: GameMap) {
        gameMap.layers.mapIndexed { i, layer ->
            inflateLayer(gameMap, layer, i)
        }

    }

    private fun inflateLayer(
        gameMap: GameMap,
        layer: GameMapTileLayer,
        i: Int
    ) {
        val tiles = Array<Array<PlacedTile?>>(gameMap.depth) { Array(gameMap.width) { null } }
        layer.tiles.mapIndexed { j, tile ->
            val index = tile.code - INITIAL_INDEX_OF_TILES_MAPPING
            if (index > 0) {
                TilesMapping.tiles[index].let { textureName ->
                    val x = j % gameMap.width
                    val z = j / gameMap.width
                    tiles[z][x] = tileFactory.createTile(textureName, x, z, i + 1)
                }
            }
        }
        val bitMap = Array(gameMap.depth) { Array(gameMap.width) { 0 } }
        tiles.forEachIndexed { z, row ->
            row.forEachIndexed { x, placedTile ->
                if (placedTile != null && placedTile.definition.surroundedTile) {
                    bitMap[z][x] = 1
                }
            }
        }
        TileLayer(name = layer.name, tiles = tiles, bitMap = bitMap).also {
            sharedData.mapData.layers.add(it)
        }
    }

    override fun update(parent: Table, deltaTime: Float) {

    }

    private fun clearMapData() {
        val layers = sharedData.mapData.layers
        layers.drop(1).forEach {
            it.tiles.forEach { tileArray ->
                tileArray.forEach { tile ->
                    if (tile != null) {
                        sharedData.mapData.modelInstances.remove(
                            tile.modelInstance
                        )
                    }
                }
            }
        }
        sharedData.mapData.placedObjects.forEach {
            sharedData.mapData.modelInstances.remove(it.modelInstance)
        }
        layers.retainAll(listOf(layers.first()).toSet())
        sharedData.mapData.placedObjects.clear()
    }

    override fun initialize() {
        super.initialize()
        addNewLayer("Deep Water", assetsManager.getTexture("tile_water"), true)
        addNewLayer(DEFAULT_LAYER_NAME, null)
        for (z in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                sharedData.mapData.layers[0].tiles[z][x]?.modelInstance?.transform?.setToTranslation(
                    x.toFloat() + 0.5F,
                    0F,
                    z.toFloat() + 0.5F
                )
            }
        }
    }

    private fun addNewLayer(name: String, texture: Texture? = null, disabled: Boolean = false): MutableList<TileLayer> {
        val layersGrid: Array<Array<PlacedTile?>> = createLayer(texture)
        val layer0 = TileLayer(name, disabled, layersGrid, Array(MAP_SIZE) { Array(MAP_SIZE) { 0 } })
        val layers = sharedData.mapData.layers
        layers.add(layer0)
        return layers
    }

    private fun createLayer(texture: Texture?): Array<Array<PlacedTile?>> {
        val layerTiles: Array<Array<PlacedTile?>> = Array(MAP_SIZE) {
            Array(MAP_SIZE) {
                if (texture != null) {
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    val modelInstance = EditorModelInstance(
                        EditorModelInstanceProps(
                            sharedData.floorModel,
                            null,
                        )
                    )
                    (modelInstance.materials.get(0)
                        .get(TextureAttribute.Diffuse) as TextureAttribute)
                        .set(TextureRegion(texture))
                    PlacedTile(modelInstance, assetsManager.getTexturesDefinitions().definitions["tile_water"]!!)
                } else null
            }
        }
        if (texture != null) {
            layerTiles.forEach {
                it.forEach { placedTile ->
                    placedTile?.let { it1 -> sharedData.mapData.modelInstances.add(it1.modelInstance) }
                }
            }
        }
        return layerTiles
    }

    companion object {
        const val DEFAULT_LAYER_NAME = "Layer 2"
    }
}
