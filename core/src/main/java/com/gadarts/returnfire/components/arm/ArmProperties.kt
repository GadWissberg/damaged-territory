package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.audio.Sound
import com.gadarts.returnfire.systems.character.factories.AimingRestriction
import com.gadarts.returnfire.systems.data.pools.RigidBodyPool

class ArmProperties(
    val damage: Int,
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float,
    val effectsData: ArmEffectsData,
    val renderData: ArmRenderData,
    val explosive: Boolean,
    val rigidBodyPool: RigidBodyPool,
    val aimingRestriction: AimingRestriction? = null,
)
