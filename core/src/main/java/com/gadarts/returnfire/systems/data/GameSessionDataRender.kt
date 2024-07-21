package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.utils.Disposable

class GameSessionDataRender : Disposable {
    lateinit var particleSystem: ParticleSystem
    lateinit var modelCache: ModelCache
    override fun dispose() {
        modelCache.dispose()
    }

}
