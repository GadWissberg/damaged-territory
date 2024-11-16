package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.GeneralUtils

class GameSessionDataRender : Disposable {
    val camera: PerspectiveCamera = GeneralUtils.createCamera()
    lateinit var particleSystem: ParticleSystem
    lateinit var modelCache: ModelCache
    override fun dispose() {
        modelCache.dispose()
    }

}
