package com.gadarts.returnfire

import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition

@Suppress("RedundantNullableReturnType", "RedundantSuppression")
object GameDebugSettings {

    const val SHOW_OBJECT_POOL_PROFILING = false
    const val SHOW_COLLISION_SHAPES = false
    const val SHOW_GL_PROFILING = true
    const val SHOW_HEAP_SIZE = false
    const val DEBUG_INPUT = false
    const val UI_DEBUG = false
    const val SFX = false
    const val DISABLE_MUSIC = true
    const val DISABLE_AMB_SOUNDS = false
    const val HIDE_PLAYER = false
    const val HIDE_FLOOR = false
    const val HIDE_ENEMIES = false
    const val FORCE_PLAYER_HP = 100000F
    const val AVOID_PARTICLE_EFFECTS_DRAWING = false
    const val ENABLE_PROFILER = true
    const val DISABLE_HUD = false
    val SELECTED_VEHICLE: CharacterDefinition? = TurretCharacterDefinition.TANK
    const val FORCE_AIM = 1
    const val AI_DISABLED = false
    const val AI_ATTACK_DISABLED = false
}
