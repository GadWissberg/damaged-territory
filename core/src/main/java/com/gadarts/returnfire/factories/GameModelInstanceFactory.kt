package com.gadarts.returnfire.factories

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils.random
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.model.GameModelInstance

class GameModelInstanceFactory(private val assetsManager: GameAssetManager) {
    fun createGameModelInstance(modelDefinition: ModelDefinition): GameModelInstance {
        assetsManager.getCachedBoundingBox(modelDefinition)
        val paths = modelDefinition.getPaths()
        val selectedIndex = random(paths.size - 1)
        val shadow =
            if (modelDefinition.separateModelForShadow) ModelInstance(
                assetsManager.get(
                    modelDefinition.shadowsPaths[selectedIndex],
                    Model::class.java
                )
            ) else null
        val gameModelInstance = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(modelDefinition, selectedIndex)),
            modelDefinition,
            shadow
        )
        return gameModelInstance
    }

}
