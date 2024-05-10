package com.gadarts.returnfire.systems.player

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Pool

class BulletsPool(private val model: Model) :
    Pool<ModelInstance>(BULLET_MODEL_INSTANCES_POOL_SIZE) {
    override fun newObject(): ModelInstance {
        return ModelInstance(model)
    }

    companion object {
        private const val BULLET_MODEL_INSTANCES_POOL_SIZE = 20
    }
}
