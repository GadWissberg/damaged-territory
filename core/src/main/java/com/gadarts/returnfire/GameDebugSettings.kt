package com.gadarts.returnfire

import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition

object GameDebugSettings {

    const val SHOW_OBJECT_POOL_PROFILING = true
    const val SHOW_COLLISION_SHAPES = false
    const val SHOW_GL_PROFILING = true
    const val DEBUG_INPUT = false
    const val UI_DEBUG = false
    const val SFX = false
    const val DISPLAY_PROPELLER = true
    const val HIDE_PLAYER = false
    const val HIDE_FLOOR = false
    val SELECTED_VEHICLE: CharacterDefinition = TurretCharacterDefinition.TANK
}
