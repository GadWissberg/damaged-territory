package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

class ModelInstanceComponent : GameComponent() {
    lateinit var modelInstance: ModelInstance
    private val boundingBox = BoundingBox()

    fun init(model: Model, position: Vector3) {
        val modelInstance = ModelInstance(model)
        initialize(modelInstance, position)
    }

    private fun initialize(
        modelInstance1: ModelInstance,
        position: Vector3,
    ) {
        this.modelInstance = modelInstance1
        this.modelInstance.transform.translate(position)
        modelInstance.calculateBoundingBox(boundingBox)
    }

    override fun reset() {
    }

    fun getBoundingBox(auxBox: BoundingBox): BoundingBox {
        return auxBox.set(boundingBox)
    }

    fun init(model: ModelInstance, position: Vector3) {
        initialize(model, position)
    }

}
