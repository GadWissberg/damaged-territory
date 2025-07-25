package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

class HangarStagesModels(assetsManager: GameAssetManager) {
    val stageTopLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val stageTopRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val stageTank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, TurretCharacterDefinition.TANK, initialPosition)
    }
    val stageApache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val stageJeep by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, SimpleCharacterDefinition.JEEP, initialPosition)
    }
    val stageBottomRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val mapping = mapOf(
        TurretCharacterDefinition.TANK to stageTank,
        SimpleCharacterDefinition.APACHE to stageApache,
        SimpleCharacterDefinition.JEEP to stageJeep,
    )
}
