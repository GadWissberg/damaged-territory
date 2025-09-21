package com.gadarts.returnfire.screens.types.hangar.scene

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class HangarCharacterModels(private val assetsManager: GameAssetManager, private val stagesModels: HangarStagesModels) {
    private val tank by lazy {
        val vehicle = createSelectableVehicle(ModelDefinition.TANK_BODY, 1.07F, -45F, stagesModels.stageTank)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_TURRET)),
                Matrix4().translate(-0.05F, 0.2F, 0F)
            )
        )
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_CANNON)),
                Matrix4().translate(0.25F, 0.17F, 0F)
            )
        )
        vehicle
    }

    private val apache by lazy {
        val vehicle = createSelectableVehicle(ModelDefinition.APACHE, 1.27F, 215F, stagesModels.stageApache)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.PROPELLER)),
                Matrix4().translate(0F, -0.02F, 0F)
            )
        )
        vehicle
    }

    private val jeep by lazy {
        val jeep = createSelectableVehicle(ModelDefinition.JEEP, 0.9F, -45F, stagesModels.stageJeep)
        val wheel = assetsManager.getAssetByDefinition(ModelDefinition.JEEP_WHEEL)
        val wheelX = 0.25F
        jeep.addChild(
            SelectableVehicleChild(
                ModelInstance(wheel),
                Matrix4().translate(wheelX, 0.1F, 0.15F).rotate(Vector3.Y, 90F)
            )
        )
        jeep.addChild(
            SelectableVehicleChild(
                ModelInstance(wheel),
                Matrix4().translate(wheelX, 0.1F, -0.15F).rotate(Vector3.Y, -90F)
            )
        )
        jeep.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.JEEP_TURRET_BASE)),
                Matrix4().translate(0F, 0.45F, 0F)
            )
        )
        jeep.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.JEEP_GUN)),
                Matrix4().translate(-0.025F, 0.5F, 0F)
            )
        )
        jeep
    }

    private fun createSelectableVehicle(
        modelDefinition: ModelDefinition,
        relativeHeight: Float,
        yaw: Float,
        vehicleElevator: VehicleElevator
    ): SelectableVehicle {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(modelDefinition))
        val vehicle = SelectableVehicle(
            modelInstance, vehicleElevator.modelInstance, relativeHeight,
            yaw
        )
        return vehicle
    }

    val vehicles by lazy {
        listOf(
            apache, tank, jeep
        )
    }

}
