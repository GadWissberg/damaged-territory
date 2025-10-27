package com.gadarts.returnfire.screens.types.hangar.scene.elevator

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

class HangarElevatorsModels(assetsManager: GameAssetManager) {
    val elevatorTopLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val elevatorTopRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val elevatorTank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, TurretCharacterDefinition.TANK, initialPosition)
    }
    val elevatorApache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val elevatorJeep by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, TurretCharacterDefinition.JEEP, initialPosition)
    }
    val elevatorBottomRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleElevator(modelInstance, SimpleCharacterDefinition.APACHE, initialPosition)
    }
    val mapping = mapOf(
        TurretCharacterDefinition.TANK to elevatorTank,
        SimpleCharacterDefinition.APACHE to elevatorApache,
        TurretCharacterDefinition.JEEP to elevatorJeep,
    )
}
