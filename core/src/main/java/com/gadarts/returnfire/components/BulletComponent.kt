package com.gadarts.returnfire.components

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.systems.player.BulletsPool

class BulletComponent : GameComponent() {
    var speed: Float = 0.0f
    val initialPosition = Vector3()
    lateinit var relatedPool: BulletsPool
    override fun reset() {

    }

    fun init(initialPosition: Vector3, speed: Float, pool: BulletsPool) {
        this.initialPosition.set(initialPosition)
        this.speed = speed
        this.relatedPool = pool
    }

}
