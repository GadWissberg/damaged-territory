package com.gadarts.returnfire.components

import com.badlogic.gdx.math.Vector3

class BulletComponent : GameComponent() {
    var speed: Float = 0.0f
    val initialPosition = Vector3()

    override fun reset() {

    }

    fun init(initialPosition: Vector3, speed: Float) {
        this.initialPosition.set(initialPosition)
        this.speed = speed
    }

}
