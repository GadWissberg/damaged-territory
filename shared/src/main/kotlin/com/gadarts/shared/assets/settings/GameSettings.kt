package com.gadarts.shared.assets.settings

import com.gadarts.shared.assets.definitions.MapDefinition
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.google.gson.annotations.SerializedName

class GameSettings(
    @SerializedName("map")
    val map: MapDefinition,
    @SerializedName("print_bit_map")
    val printBitMap: Boolean = false,
    @SerializedName("show_object_pool_profiling")
    val showObjectPoolProfiling: Boolean = false,
    @SerializedName("show_collision_shapes")
    val showCollisionShapes: Boolean = false,
    @SerializedName("show_gl_profiling")
    val showGlProfiling: Boolean = false,
    @SerializedName("show_heap_size")
    val showHeapSize: Boolean = false,
    @SerializedName("show_axis")
    val showAxis: Boolean = false,
    @SerializedName("debug_input")
    val debugInput: Boolean = false,
    @SerializedName("ui_debug")
    val uiDebug: Boolean = false,
    @SerializedName("sfx")
    val sfx: Boolean = false,
    @SerializedName("disable_music")
    val disableMusic: Boolean = false,
    @SerializedName("disable_amb_sounds")
    val disableAmbSounds: Boolean = false,
    @SerializedName("hide_player")
    val hidePlayer: Boolean = false,
    @SerializedName("hide_floor")
    val hideFloor: Boolean = false,
    @SerializedName("hide_enemies")
    val hideEnemies: Boolean = false,
    @SerializedName("hide_amb_objects")
    val hideAmbObjects: Boolean = false,
    @SerializedName("hide_bullet_holes")
    val hideBulletHoles: Boolean = false,
    @SerializedName("render_only_first_floor_layer")
    val renderOnlyFirstFloorLayer: Boolean = false,
    @SerializedName("force_player_hp")
    val forcePlayerHp: Float = -1F,
    @SerializedName("force_enemy_hp")
    val forceEnemyHp: Float = -1F,
    @SerializedName("force_ammo")
    val forceAmmo: Int = -1,
    @SerializedName("force_initial_fuel")
    val forceInitialFuel: Float = -1F,
    @SerializedName("avoid_particle_effects_drawing")
    val avoidParticleEffectsDrawing: Boolean = false,
    @SerializedName("enable_profiler")
    val enableProfiler: Boolean = false,
    @SerializedName("disable_hud")
    val disableHud: Boolean = false,
    @SerializedName("selected_vehicle")
    val selectedVehicle: CharacterDefinition? = null,
    @SerializedName("selected_vehicle_ai")
    val selectedVehicleAi: CharacterDefinition? = null,
    @SerializedName("force_aim")
    val forceAim: Int = 0,
    @SerializedName("ai_disabled")
    val aiDisabled: Boolean = false,
    @SerializedName("ai_vehicle_disabled")
    val aiVehicleDisabled: Boolean = false,
    @SerializedName("ai_attack_disabled")
    val aiAttackDisabled: Boolean = false,
    @SerializedName("ai_avoid_going_back_to_base")
    val aiAvoidGoingBackToBase: Boolean = false,
    @SerializedName("ai_force_thrust")
    val aiForceThrust: Boolean = false,
    @SerializedName("ai_show_path_nodes")
    val aiShowPathNodes: Boolean = false,
    @SerializedName("use_debug_dll")
    val useDebugDll: Boolean = false,
    @SerializedName("force_gibs")
    val forceGibs: Boolean = false
)


