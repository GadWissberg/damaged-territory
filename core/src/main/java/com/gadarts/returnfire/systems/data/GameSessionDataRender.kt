package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.systems.data.GameSessionData.Companion.FOV

class GameSessionDataRender : Disposable {
    val camera: PerspectiveCamera = PerspectiveCamera(
        FOV,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    )
    lateinit var particleSystem: ParticleSystem
    lateinit var modelCache: ModelCache
    override fun dispose() {
        modelCache.dispose()
    }

}
