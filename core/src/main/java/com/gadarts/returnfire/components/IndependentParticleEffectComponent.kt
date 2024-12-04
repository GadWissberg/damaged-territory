package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool.Poolable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

class ParticleEffectComponent : Component, Poolable {

    var createdAt: Long = 0
        private set
    var ttlInSeconds: Int = 0
        private set
    var parent: Entity? = null
    lateinit var definition: ParticleEffectDefinition
    lateinit var effect: ParticleEffect
        private set
    val parentRelativePosition: Vector3 = Vector3()

    fun init(
        effect: ParticleEffect,
        definition: ParticleEffectDefinition,
        parent: Entity?,
        ttlInSeconds: Int,
        parentRelativePosition: Vector3 = Vector3.Zero,
    ) {
        this.effect = effect
        this.definition = definition
        this.parent = parent
        this.ttlInSeconds = ttlInSeconds
        this.parentRelativePosition.set(parentRelativePosition)
        this.createdAt = TimeUtils.millis()
    }

    override fun reset() {
    }

}


