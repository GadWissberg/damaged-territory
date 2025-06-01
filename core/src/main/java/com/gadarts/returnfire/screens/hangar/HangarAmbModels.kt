package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class HangarAmbModels(assetsManager: GameAssetManager) {
    val ceilingModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.CEILING))
        modelInstance.transform.setToTranslation(Vector3(0.9F, 11F, 5F))
        modelInstance
    }

    val sceneModelInstance by lazy {
        ModelInstance(
            assetsManager.getAssetByDefinition(
                ModelDefinition.SCENE
            )
        )
    }
    val hookModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.HOOK))
        modelInstance.transform.setToTranslation(Vector3(-1.5F, 5.2F, 4.3F))
        modelInstance
    }
    val fanModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.FAN))
        modelInstance.transform.setToTranslation(Vector3(0.9F, 0.1F, 4.4F))
        modelInstance
    }
    val ceilingFanModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.FAN))
        modelInstance.transform.setToTranslation(Vector3(-1.1F, 11F, 3.3F)).scl(4.5F)
        modelInstance
    }
}
