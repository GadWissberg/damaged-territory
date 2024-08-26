package com.gadarts.returnfire.components.bullet

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.components.GameComponent

class BulletComponent : GameComponent() {
    lateinit var explosion: ParticleEffect
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set

    override fun reset() {

    }

    fun init(behavior: BulletBehavior, explosion: ParticleEffect) {
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
    }

}
