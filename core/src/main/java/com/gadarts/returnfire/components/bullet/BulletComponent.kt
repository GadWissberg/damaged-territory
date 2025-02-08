package com.gadarts.returnfire.components.bullet

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

class BulletComponent : Component, Poolable {
    var damage: Float = 0F
        private set
    var friendly: Boolean = false
        private set
    var explosive: Boolean = false
        private set
    var explosion: ParticleEffectDefinition? = null
        private set
    var createdTime: Long = 0
        private set
    lateinit var behavior: BulletBehavior
        private set

    override fun reset() {

    }

    fun init(
        behavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean,
        damage: Float
    ) {
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
        this.explosive = explosive
        this.friendly = friendly
        this.damage = damage
    }

}
