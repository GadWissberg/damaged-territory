package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.utils.GeneralUtils

class GameSessionDataRender : Disposable {
    val cameraRelativePosition = Vector3()
    val cameraRelativeLookAtPosition = Vector3()
    val cameraRelativeTargetPosition = Vector3()
    val cameraRelativeTargetLookAtPosition = Vector3()
    val camera: PerspectiveCamera = GeneralUtils.createCamera(60F)
    lateinit var particleSystem: ParticleSystem
    lateinit var modelCache: ModelCache
    lateinit var floorModel: Model

    override fun dispose() {
        modelCache.dispose()
        floorModel.dispose()
    }


}
