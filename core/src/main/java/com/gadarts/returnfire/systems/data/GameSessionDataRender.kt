package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.utils.GeneralUtils

class GameSessionDataRender : Disposable {
    val camera: PerspectiveCamera = GeneralUtils.createCamera(67F)
    lateinit var particleSystem: ParticleSystem
    lateinit var modelCache: ModelCache
    override fun dispose() {
        modelCache.dispose()
        floorModel.dispose()
    }

    lateinit var floorModel: Model

}
