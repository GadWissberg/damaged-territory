package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.assets.ModelDefinition

class ModelInstanceComponent : GameComponent() {
    var hidden: Boolean = false
    lateinit var gameModelInstance: GameModelInstance

    fun init(
        model: Model,
        position: Vector3,
        modelDefinition: ModelDefinition,
        boundingBox: BoundingBox
    ) {
        val modelInstance = GameModelInstance(ModelInstance(model), modelDefinition, boundingBox)
        initialize(modelInstance, position)
    }

    fun init(
        model: GameModelInstance,
        position: Vector3
    ) {
        initialize(model, position)
    }

    private fun initialize(
        modelInstance: GameModelInstance,
        position: Vector3,
    ) {
        modelInstance.modelInstance.transform.idt()
        this.gameModelInstance = modelInstance
        this.gameModelInstance.modelInstance.transform.translate(position)
        this.gameModelInstance.updateBoundingBoxPosition()
    }


    override fun reset() {
    }

}
