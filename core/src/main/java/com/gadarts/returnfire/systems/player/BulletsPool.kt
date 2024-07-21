package com.gadarts.returnfire.systems.player

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.model.GameModelInstance

class BulletsPool(
    private val model: Model,
    private val boundingBox: BoundingBox,
    private val modelDefinition: ModelDefinition
) :
    Pool<GameModelInstance>(BULLET_MODEL_INSTANCES_POOL_SIZE) {
    override fun newObject(): GameModelInstance {
        return GameModelInstance(ModelInstance(model), modelDefinition, boundingBox)
    }

    companion object {
        private const val BULLET_MODEL_INSTANCES_POOL_SIZE = 20
    }
}
