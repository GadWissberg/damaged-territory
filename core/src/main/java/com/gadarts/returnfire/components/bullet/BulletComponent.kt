package com.gadarts.returnfire.components.bullet

import com.gadarts.returnfire.components.GameComponent
import com.gadarts.returnfire.systems.player.BulletsPool

class BulletComponent : GameComponent() {
    private lateinit var behavior: BulletBehavior
    lateinit var relatedPool: BulletsPool
    override fun reset() {

    }

    fun init(pool: BulletsPool, behavior: BulletBehavior) {
        this.relatedPool = pool
        this.behavior = behavior
    }

}
