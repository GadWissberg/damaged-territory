package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.gadarts.returnfire.assets.definitions.ModelDefinition

class ArmProperties(
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float,
    val radius: Float,
    val explosion: ParticleEffect,
    val modelDefinition: ModelDefinition
)
