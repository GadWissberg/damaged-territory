package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.GameComponent
import com.gadarts.returnfire.systems.player.BulletsPool

class BulletComponent : GameComponent() {
    var explosion: ParticleEffect? = null
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set
    lateinit var relatedPool: BulletsPool
        private set

    override fun reset() {

    }

    fun init(pool: BulletsPool, behavior: BulletBehavior, explosion: ParticleEffect? = null) {
        this.relatedPool = pool
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
    }

}
