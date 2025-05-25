package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.model.ModelDefinition
import com.gadarts.returnfire.managers.GameAssetManager

class HangarCharacterModels(assetsManager: GameAssetManager, private val stagesModels: HangarStagesModels) {
    val tank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_BODY))
        val vehicle = SelectableVehicle(modelInstance, stagesModels.stageTank.modelInstance, 1.07F, -45F)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_TURRET)),
                Vector3(-0.05F, 0.2F, 0F)
            )
        )
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_CANNON)),
                Vector3(0.25F, 0.17F, 0F)
            )
        )
        vehicle
    }
    val apache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.APACHE))
        val vehicle = SelectableVehicle(modelInstance, stagesModels.stageApache.modelInstance, 1.27F, 215F)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.PROPELLER)),
                Vector3(0F, -0.02F, 0F)
            )
        )
        vehicle
    }
}
