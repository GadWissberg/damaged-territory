package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.audio.Sound

enum class SoundDefinition(fileNames: Int = 1) : AssetDefinition<Sound> {

    PROPELLER,
    PROPELLER_START,
    PROPELLER_STOP,
    MACHINE_GUN,
    MISSILE,
    AMB_WIND(2),
    AMB_EAGLE,
    AMB_OUD(3),
    AMB_FIRE,
    CRASH(2),
    EXPLOSION(3),
    EXPLOSION_SMALL,
    CANNON_A,
    CANNON_B,
    CANNON_C,
    WATER_SPLASH(3),
    ENGINE,
    STAGE_DEPLOY,
    STAGE_MOVE,
    BASE_DOOR_MOVE,
    BASE_DOOR_DONE,
    CRASH_BIG,
    PLANE_CRASH,
    TREE_FALL,
    MAJOR_IMPACT(3),
    ROCKS(2),
    FALLING_BUILDING,
    EXPLOSION_HUGE,
    METAL_BEND,
    METAL_CRASH,
    METAL_LIGHT;

    private val paths = ArrayList<String>()

    init {
        initializePaths("sfx/%s.wav", getPaths(), fileNames)
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<Sound>? {
        return null
    }

    override fun getClazz(): Class<Sound> {
        return Sound::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }
}
