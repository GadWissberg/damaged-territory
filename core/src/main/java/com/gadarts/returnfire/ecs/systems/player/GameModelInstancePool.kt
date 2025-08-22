package com.gadarts.returnfire.ecs.systems.player

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class GameModelInstancePool(
    private val model: Model,
    private val modelDefinition: ModelDefinition
) :
    Pool<GameModelInstance>(BULLET_MODEL_INSTANCES_POOL_SIZE) {
    override fun newObject(): GameModelInstance {
        return GameModelInstance(ModelInstance(model), modelDefinition)
    }

    companion object {
        private const val BULLET_MODEL_INSTANCES_POOL_SIZE = 20
    }
}
