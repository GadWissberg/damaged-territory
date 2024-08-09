package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.GameComponent
import com.gadarts.returnfire.systems.player.BulletsPool

class BulletComponent : GameComponent() {
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set
    lateinit var relatedPool: BulletsPool
        private set

    override fun reset() {

    }

    fun init(pool: BulletsPool, behavior: BulletBehavior) {
        this.relatedPool = pool
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
    }

}
