package com.gadarts.returnfire.assets

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect

enum class ParticleEffectDefinition(fileNames: Int = 1) : AssetDefinition<ParticleEffect> {

    EXPLOSION_GROUND;

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
