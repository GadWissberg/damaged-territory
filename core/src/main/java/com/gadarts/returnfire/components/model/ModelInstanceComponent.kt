package com.gadarts.returnfire.components.model

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.GameComponent

class ModelInstanceComponent : GameComponent() {
    var hidden: Boolean = false
    lateinit var gameModelInstance: GameModelInstance

    fun init(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        calculateBoundingBox: Boolean,
        direction: Float
    ) {
        gameModelInstance.modelInstance.transform.idt()
        this.gameModelInstance = gameModelInstance
        this.gameModelInstance.modelInstance.transform.translate(position)
        this.gameModelInstance.modelInstance.transform.rotate(Vector3.Y, direction)
        if (calculateBoundingBox) {
            this.gameModelInstance.calculateBoundingBox()
        }
    }


    override fun reset() {
    }

}