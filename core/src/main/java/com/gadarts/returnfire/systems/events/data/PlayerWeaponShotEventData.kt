package com.gadarts.returnfire.systems.events.data

import com.gadarts.returnfire.components.bullet.BulletBehavior

object PlayerWeaponShotEventData {
    lateinit var behavior: BulletBehavior
        private set

    fun set(behavior: BulletBehavior) {
        this.behavior = behavior
    }


}
