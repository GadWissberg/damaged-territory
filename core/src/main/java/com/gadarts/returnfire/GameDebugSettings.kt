package com.gadarts.returnfire

import com.gadarts.shared.assets.definitions.MapDefinition
import com.gadarts.shared.model.definitions.CharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

@Suppress("RedundantNullableReturnType", "RedundantSuppression")
object GameDebugSettings {

    val MAP = MapDefinition.MAP_0
    const val PRINT_BIT_MAP: Boolean = false
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
    const val FORCE_PLAYER_HP: Float = 10000F
    const val FORCE_ENEMY_HP: Float = -1F
    const val FORCE_AMMO: Int = -1
    const val FORCE_INITIAL_FUEL: Float = -1F
    const val AVOID_PARTICLE_EFFECTS_DRAWING = false
    const val ENABLE_PROFILER = true
    const val DISABLE_HUD = false
    val SELECTED_VEHICLE: CharacterDefinition? = null
    val SELECTED_VEHICLE_AI: CharacterDefinition? = TurretCharacterDefinition.TANK
    const val FORCE_AIM = 1
    const val AI_DISABLED = false
    const val AI_ATTACK_DISABLED = false
    const val USE_DEBUG_DLL = false
}
