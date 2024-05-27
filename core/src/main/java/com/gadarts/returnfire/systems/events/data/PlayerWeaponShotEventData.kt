package com.gadarts.returnfire.systems.events.data

import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.systems.player.BulletsPool

object PlayerWeaponShotEventData {
    lateinit var behavior: BulletBehavior
        private set
    lateinit var pool: BulletsPool
        private set

    fun set(pool: BulletsPool, behavior: BulletBehavior) {
        this.pool = pool
        this.behavior = behavior
    }


}
