package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.components.GameComponent
import com.gadarts.returnfire.systems.player.BulletsPool

class BulletComponent : GameComponent() {
    private lateinit var behavior: BulletBehavior
    var speed: Float = 0.0f
        private set
    val initialPosition = Vector3()
    lateinit var relatedPool: BulletsPool
    override fun reset() {

    }

    fun init(initialPosition: Vector3, speed: Float, pool: BulletsPool, behavior: BulletBehavior) {
        this.initialPosition.set(initialPosition)
        this.speed = speed
        this.relatedPool = pool
        this.behavior = behavior
    }

}
