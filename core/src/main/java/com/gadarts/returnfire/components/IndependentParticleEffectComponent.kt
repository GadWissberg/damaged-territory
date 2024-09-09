package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

class ParticleEffectComponent : GameComponent() {

    var parent: Entity? = null
    lateinit var definition: ParticleEffectDefinition
    lateinit var effect: ParticleEffect
        private set
    val parentRelativePosition: Vector3 = Vector3()

    override fun reset() {
    }

    fun init(
        effect: ParticleEffect,
        definition: ParticleEffectDefinition,
        parent: Entity?,
        parentRelativePosition: Vector3 = Vector3.Zero
    ) {
        this.effect = effect
        this.definition = definition
        this.parent = parent
        this.parentRelativePosition.set(parentRelativePosition)
    }

}


