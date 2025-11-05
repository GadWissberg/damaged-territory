package com.gadarts.dte

import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Quaternion
import com.gadarts.dte.scene.PlacedObject
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.dte.scene.handlers.render.EditorModelInstanceProps
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.data.definitions.AmbDefinition
import com.gadarts.shared.data.definitions.ElementDefinition

class ObjectFactory(private val sharedData: SharedData, private val gameAssetsManager: GameAssetManager) {
    fun addObject(x: Int, z: Int, elementDefinition: ElementDefinition, rotation: Quaternion? = null) {
        val definition = elementDefinition.getModelDefinition()
        val finalPositionX = x.toFloat() + 0.5F
        val finalPositionY = 0.07f
        val finalPositionZ = z.toFloat() + 0.5F
        val relatedModelToBeRenderedInEditors = createRelatedModelToBeRenderedInEditor(
            elementDefinition,
            finalPositionX,
            finalPositionY,
            finalPositionZ
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
        sharedData.mapData.placedObjects.add(PlacedObject(z, x, elementDefinition, modelInstance))
        applyCustomTexture(elementDefinition, modelInstance)
    }

    private fun applyCustomTexture(
        elementDefinition: ElementDefinition,
        modelInstance: EditorModelInstance
    ) {
        if (elementDefinition is AmbDefinition) {
            val customTexture = elementDefinition.customTexture
            if (customTexture != null) {
                SharedUtils.applyCustomTextureToModelInstance(
                    gameAssetsManager,
                    modelInstance,
                    customTexture
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun createRelatedModelToBeRenderedInEditor(
        elementDefinition: ElementDefinition,
        finalPositionX: Float,
        finalPositionY: Float,
        finalPositionZ: Float
    ) = if (elementDefinition is AmbDefinition) elementDefinition.relatedModelsToBeRenderedInEditor.map {
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
        val customTexture = it.customTexture
        if (customTexture != null) {
            (editorModelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                gameAssetsManager.getTexture(customTexture)
        }
        editorModelInstance
    } else null
}
