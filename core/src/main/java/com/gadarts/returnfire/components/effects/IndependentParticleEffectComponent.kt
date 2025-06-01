package com.gadarts.returnfire.components.effects

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool.Poolable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition

class ParticleEffectComponent : Component, Poolable {

    var ttlForComponentOnly: Boolean = false
        private set
    var createdAt: Long = 0
        private set
    var ttlInSeconds: Int = 0
        private set
    var followEntity: Entity? = null
    lateinit var definition: ParticleEffectDefinition
    lateinit var effect: ParticleEffect
        private set
    val followRelativePosition: Vector3 = Vector3()

    fun init(
        effect: ParticleEffect,
        definition: ParticleEffectDefinition,
        ttlInSeconds: Int,
        ttlForComponentOnly: Boolean = false,
        followRelativePosition: Vector3 = Vector3.Zero,
        followEntity: Entity?,
    ) {
        this.effect = effect
        this.definition = definition
        this.followEntity = followEntity
        this.ttlInSeconds = ttlInSeconds
        this.ttlForComponentOnly = ttlForComponentOnly
        this.followRelativePosition.set(followRelativePosition)
        this.createdAt = TimeUtils.millis()
    }

    override fun reset() {
    }

}


