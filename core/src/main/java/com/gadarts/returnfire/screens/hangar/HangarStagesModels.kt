package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.managers.GameAssetManager

class HangarStagesModels(assetsManager: GameAssetManager) {
    val stageTopLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    val stageTopRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    val stageTank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    val stageApache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    val stageBottomLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    val stageBottomRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }

}
