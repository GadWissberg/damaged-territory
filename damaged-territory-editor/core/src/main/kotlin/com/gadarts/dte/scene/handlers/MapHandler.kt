package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.gadarts.dte.EditorEvents
import com.gadarts.dte.SharedData
import com.gadarts.dte.TileLayer
import com.gadarts.dte.scene.SceneRenderer.Companion.MAP_SIZE
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class MapHandler(
    private val sharedData: SharedData,
    private val assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher
) : SceneHandler(dispatcher) {
    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        SharedUtils.createFlatMesh(builder, "floor", 0.5F, null, 0F)
        return builder.end()
    }

    override val subscribedEvents: Map<EditorEvents, EditorOnEvent> = mapOf(
        EditorEvents.ADD_LAYER to object : EditorOnEvent {
            override fun react(msg: com.badlogic.gdx.ai.msg.Telegram) {
                addNewLayer(msg.extraInfo as String, null)
                Gdx.app.log(
                    "MapHandler",
                    "Added new layer: ${msg.extraInfo} (total layers: ${sharedData.layers.size})"
                )
            }
        },
    )

    init {
        sharedData.floorModel = createFloorModel()
    }

    override fun update(parent: Table, deltaTime: Float) {

    }

    override fun initialize() {
        super.initialize()
        addNewLayer("Deep Water", assetsManager.getTexture("tile_water"), true)
        addNewLayer("Layer 2", null)
        for (z in 0 until MAP_SIZE) {
            for (x in 0 until MAP_SIZE) {
                sharedData.layers[0].tiles[z][x]?.transform?.setToTranslation(
                    x.toFloat() + 0.5F,
                    0F,
                    z.toFloat() + 0.5F
                )
            }
        }
    }

    private fun addNewLayer(name: String, texture: Texture? = null, disabled: Boolean = false): MutableList<TileLayer> {
        val layersGrid: Array<Array<ModelInstance?>> = createLayer(texture)
        val layer0 = TileLayer(name, disabled, layersGrid, Array(MAP_SIZE) { Array(MAP_SIZE) { 0 } })
        val layers = sharedData.layers
        layers.add(layer0)
        return layers
    }

    private fun createLayer(texture: Texture?): Array<Array<ModelInstance?>> {
        val layerTiles: Array<Array<ModelInstance?>> = Array(MAP_SIZE) {
            Array(MAP_SIZE) {
                if (texture != null) {
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    val modelInstance = ModelInstance(sharedData.floorModel)
                    (modelInstance.materials.get(0)
                        .get(TextureAttribute.Diffuse) as TextureAttribute)
                        .set(TextureRegion(texture))
                    modelInstance
                } else null
            }
        }
        if (texture != null) {
            sharedData.modelInstances.addAll(layerTiles.flatten().filterNotNull())
        }
        return layerTiles
    }

}
