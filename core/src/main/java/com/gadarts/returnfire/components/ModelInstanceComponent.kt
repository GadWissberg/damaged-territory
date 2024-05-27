package com.gadarts.returnfire.components

import com.badlogic.gdx.math.Vector3

class ModelInstanceComponent : GameComponent() {
    var hidden: Boolean = false
    lateinit var gameModelInstance: GameModelInstance

    fun init(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        calculateBoundingBox: Boolean
    ) {
        gameModelInstance.modelInstance.transform.idt()
        this.gameModelInstance = gameModelInstance
        this.gameModelInstance.modelInstance.transform.translate(position)
        if (calculateBoundingBox) {
            this.gameModelInstance.calculateBoundingBox()
        }
    }


    override fun reset() {
    }

}
