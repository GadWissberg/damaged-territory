package com.gadarts.returnfire.factories

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils.random
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo

class GameModelInstanceFactory(private val assetsManager: GameAssetManager) {
    fun createGameModelInstance(modelDefinition: ModelDefinition, customTexture: String? = null): GameModelInstance {
        val paths = modelDefinition.getPaths()
        val selectedIndex = random(paths.size - 1)
        val shadow =
            if (modelDefinition.separateModelForShadow) ModelInstance(
                assetsManager.get(
                    modelDefinition.shadowsPaths[selectedIndex],
                    Model::class.java
                )
            ) else null
        val modelInstance = ModelInstance(assetsManager.getAssetByDefinition(modelDefinition, selectedIndex))
        if (customTexture != null) {
            SharedUtils.applyCustomTextureToModelInstance(
                assetsManager,
                modelInstance,
                customTexture,
                modelDefinition.mainMaterialIndex
            )
        }
        val gameModelInstance = GameModelInstance(
            modelInstance,
            ImmutableGameModelInstanceInfo(modelDefinition, if (paths.size > 1) selectedIndex else null),
            shadow,
        )
        return gameModelInstance
    }

}
