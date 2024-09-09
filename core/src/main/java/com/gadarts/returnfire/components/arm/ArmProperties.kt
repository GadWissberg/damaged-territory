package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.audio.Sound
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.systems.data.GameParticleEffectPool

class ArmProperties(
    val damage: Int,
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float,
    val radius: Float,
    val explosion: ParticleEffectDefinition?,
    val modelDefinition: ModelDefinition,
    val smokeEmit: GameParticleEffectPool?,
    val sparkParticleEffect: GameParticleEffectPool,
    val smokeTrail: GameParticleEffectPool?,
    val explosive: Boolean,
    val initialRotationAroundZ: Float = 0F
)
