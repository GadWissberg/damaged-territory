package com.gadarts.returnfire.systems.player

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.ModelDefinition
import com.gadarts.returnfire.components.GameModelInstance

class BulletsPool(
    private val model: Model,
    private val modelDefinition: ModelDefinition,
    private val boundingBox: BoundingBox
) :
    Pool<GameModelInstance>(BULLET_MODEL_INSTANCES_POOL_SIZE) {
    override fun newObject(): GameModelInstance {
        return GameModelInstance(ModelInstance(model), modelDefinition, boundingBox)
    }

    companion object {
        private const val BULLET_MODEL_INSTANCES_POOL_SIZE = 20
    }
}
