package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.shared.SharedUtils

class HangarSceneModelsData(assetsManager: GameAssetManager) {
    val camera by lazy { SharedUtils.createCamera(55F) }
    val batch by lazy { ModelBatch() }

    val stagesModels = HangarStagesModels(assetsManager)
    val hangarAmbModels = HangarAmbModels(assetsManager)
    val charactersModels = HangarCharacterModels(assetsManager, stagesModels)
}
