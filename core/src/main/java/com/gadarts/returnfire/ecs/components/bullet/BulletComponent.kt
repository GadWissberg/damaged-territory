package com.gadarts.returnfire.ecs.components.bullet

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool.Poolable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.data.CharacterColor

class BulletComponent : Component, Poolable {
    var destroyOnSky: Boolean = false
        private set
    var destroyed: Boolean = false
        private set
    var damage: Float = 0F
        private set
    var color: CharacterColor = CharacterColor.BROWN
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
        color: CharacterColor,
        damage: Float,
        destroyOnSky: Boolean
    ) {
        this.behavior = behavior
        this.createdTime = TimeUtils.millis()
        this.explosion = explosion
        this.explosive = explosive
        this.color = color
        this.damage = damage
        this.destroyed = false
        this.destroyOnSky = destroyOnSky
    }

    fun markAsDestroyed() {
        destroyed = true
    }

}
