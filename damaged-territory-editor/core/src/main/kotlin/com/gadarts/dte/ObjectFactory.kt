package com.gadarts.dte

import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Quaternion
import com.gadarts.dte.scene.PlaceableObject
import com.gadarts.dte.scene.PlacedObject
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.scene.handlers.render.EditorModelInstanceProps
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.AmbDefinition

class ObjectFactory(private val sharedData: SharedData, private val gameAssetsManager: GameAssetManager) {
    fun addObject(
        x: Int,
        z: Int,
        placeableObject: PlaceableObject,
        rotation: Quaternion? = null,
    ) {
        val elementDefinition = placeableObject.definition
        val definition = elementDefinition.getModelDefinition()
        val finalPositionX = x.toFloat() + 0.5F
        val finalPositionY = 0.07f
        val finalPositionZ = z.toFloat() + 0.5F
        val relatedModelToBeRenderedInEditors = createRelatedModelToBeRenderedInEditor(
            placeableObject,
            finalPositionX,
            finalPositionY,
            finalPositionZ,
        )
        val modelInstance = EditorModelInstance(
            EditorModelInstanceProps(
                gameAssetsManager.getAssetByDefinition(definition),
                definition,
                relatedModelToBeRenderedInEditors
            )
        )
        sharedData.mapData.modelInstances.add(modelInstance)
        modelInstance.transform.setToTranslation(finalPositionX, finalPositionY, finalPositionZ)
        if (rotation != null) {
            modelInstance.transform.rotate(rotation)
        }
        sharedData.mapData.placedObjects.add(
            PlacedObject(
                z,
                x,
                elementDefinition,
                modelInstance,
                placeableObject.color
            )
        )
        applyCustomTexture(placeableObject, modelInstance)
    }

    private fun applyCustomTexture(
        placeableObject: PlaceableObject,
        modelInstance: EditorModelInstance
    ) {
        val definition = placeableObject.definition
        val definitionPerColor = definition.customTexturePerColor()
        if (definitionPerColor != null && placeableObject.color != null) {
            SharedUtils.applyCustomTextureToModelInstance(
                gameAssetsManager,
                modelInstance,
                "${definitionPerColor}_${placeableObject.color.name.lowercase()}"
            )
        }
    }

    @Suppress("SameParameterValue")
    private fun createRelatedModelToBeRenderedInEditor(
        placeableObject: PlaceableObject,
        finalPositionX: Float,
        finalPositionY: Float,
        finalPositionZ: Float,
    ) =
        if (placeableObject.definition is AmbDefinition) placeableObject.definition.relatedModelsToBeRenderedInEditor.map {
            val relatedModelDefinition = it.modelDefinition
            val editorModelInstance = EditorModelInstance(
                EditorModelInstanceProps(
                    gameAssetsManager.getAssetByDefinition(
                        relatedModelDefinition
                    ),
                    relatedModelDefinition, null
                )
            )
            editorModelInstance.transform.set(it.relativeTransform).trn(
                finalPositionX,
                finalPositionY,
                finalPositionZ
            )
            var customTexture = it.customTexture
            if (customTexture != null) {
                val color: CharacterColor? = placeableObject.color
                if (color != null) {
                    customTexture = customTexture + "_" + color.name.lowercase()
                }
                (editorModelInstance.materials.get(0)
                    .get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                    gameAssetsManager.getTexture(customTexture)
            }
            editorModelInstance
        } else null
}
