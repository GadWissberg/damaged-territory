package com.gadarts.dte

import com.gadarts.dte.scene.PlacedObject
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.EditorModelInstance
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.model.definitions.ElementDefinition

class ObjectFactory(private val sharedData: SharedData, private val gameAssetsManager: GameAssetManager) {
    fun addObject(x: Int, z: Int, elementDefinition: ElementDefinition): Boolean {
        val modelInstance = EditorModelInstance(
            gameAssetsManager.getAssetByDefinition(elementDefinition.getModelDefinition())
        )
        sharedData.modelInstances.add(modelInstance)
        modelInstance.transform.setToTranslation(
            x.toFloat() + 0.5F,
            0.07f,
            z.toFloat() + 0.5F
        )
        sharedData.placedObjects.add(
            PlacedObject(
                z,
                x,
                elementDefinition,
                modelInstance
            )
        )
        return true
    }

}
