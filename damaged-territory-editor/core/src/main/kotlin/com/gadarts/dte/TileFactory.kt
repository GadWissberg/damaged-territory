package com.gadarts.dte

import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.scene.handlers.render.EditorModelInstanceProps
import com.gadarts.shared.GameAssetManager

class TileFactory(private val sharedData: SharedData, private val gameAssetsManager: GameAssetManager) {
    fun addTile(textureName: String, tileLayer: TileLayer, x: Int, z: Int): PlacedTile {
        val placedTile = createTile(textureName, x, z, sharedData.mapData.layers.indexOf(tileLayer))
        tileLayer.tiles[z][x] = placedTile
        return placedTile
    }

    fun createTile(
        textureName: String,
        x: Int,
        z: Int,
        layerIndex: Int,
    ): PlacedTile {
        val modelInstance = EditorModelInstance(EditorModelInstanceProps(sharedData.floorModel, null))
        val placedTile =
            PlacedTile(modelInstance, gameAssetsManager.getTexturesDefinitions().definitions[textureName]!!)
        sharedData.mapData.modelInstances.add(modelInstance)
        initializeTile(textureName, x, layerIndex, z, placedTile)
        return placedTile
    }

    private fun initializeTile(
        textureName: String,
        x: Int,
        y: Int,
        z: Int,
        placedTile: PlacedTile
    ) {
        val texture = gameAssetsManager.getTexture(textureName)
        val modelInstance = placedTile.modelInstance
        (modelInstance.materials[0].get(
            TextureAttribute.Diffuse
        ) as TextureAttribute
                ).textureDescription.texture = texture
        modelInstance.transform.setToTranslation(
            x.toFloat() + 0.5F,
            y * 0.01F,
            z.toFloat() + 0.5F
        )
    }

}
