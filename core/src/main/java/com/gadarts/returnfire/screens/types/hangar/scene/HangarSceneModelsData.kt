package com.gadarts.returnfire.screens.types.hangar.scene

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.gadarts.returnfire.screens.types.hangar.scene.elevator.HangarElevatorsModels
import com.gadarts.returnfire.screens.types.hangar.scene.vehicles.HangarVehicleModels
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils

class HangarSceneModelsData(assetsManager: GameAssetManager) {
    val camera by lazy { SharedUtils.createCamera(55F) }
    val batch by lazy { ModelBatch() }

    val stagesModels = HangarElevatorsModels(assetsManager)
    val hangarAmbModels = HangarAmbModels(assetsManager)
    val charactersModels = HangarVehicleModels(assetsManager, stagesModels)
}
