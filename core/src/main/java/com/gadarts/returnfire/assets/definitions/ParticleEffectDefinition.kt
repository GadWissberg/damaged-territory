package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect

enum class ParticleEffectDefinition(fileNames: Int = 1, val hasBlastRing: Boolean = false, val loop: Boolean = false) :
    AssetDefinition<ParticleEffect> {

    EXPLOSION_GROUND(hasBlastRing = true),
    EXPLOSION_SMALL(hasBlastRing = true),
    EXPLOSION(hasBlastRing = true),
    SMOKE,
    SMOKE_SMALL,
    SMOKE_SMALL_LOOP(loop = true),
    SMOKE_MED,
    SMOKE_LOOP(loop = true),
    SMOKE_UP_LOOP(loop = true),
    SMOKE_EMIT,
    WATER_SPLASH,
    SPARK_SMALL,
    RICOCHET;

    private val paths = ArrayList<String>()
    private val pathFormat = "particles/%s.pfx"

    init {
        initializePaths(pathFormat, fileNames)
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
