package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect

enum class ParticleEffectDefinition(fileNames: Int = 1, val hasBlastRing: Boolean = false) :
    AssetDefinition<ParticleEffect> {

    EXPLOSION_SMALL(hasBlastRing = true),
    EXPLOSION(hasBlastRing = true),
    EXPLOSION_MED(hasBlastRing = true),
    SMOKE,
    SMOKE_SMALL,
    SMOKE_TINY,
    SMOKE_SMALL_LOOP,
    SMOKE_MED,
    SMOKE_LOOP,
    SMOKE_LOOP_BIG,
    SMOKE_BIG_RECTANGLE,
    SMOKE_LOOP_HUGE,
    SMOKE_UP_LOOP,
    SMOKE_EMIT,
    SMOKE_BROWN,
    WATER_SPLASH,
    SPARK_SMALL,
    SPARK_NO_SMOKE,
    SPARK_MED,
    RICOCHET,
    FIRE_LOOP,
    FIRE_LOOP_SMALL;

    private val paths = ArrayList<String>()
    private val pathFormat = "particles/%s.pfx"

    init {
        initializePaths(pathFormat, getPaths(), fileNames)
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<ParticleEffect>? {
        return null
    }

    override fun getClazz(): Class<ParticleEffect> {
        return ParticleEffect::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }

}
