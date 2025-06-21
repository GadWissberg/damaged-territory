package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader.AlignMode
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.systems.camera.CameraState
import com.gadarts.shared.SharedUtils

class GameSessionDataRender : Disposable {
    var lastCameraStateChange: Long = 0
    var cameraState: CameraState = CameraState.FOCUS_DEPLOYMENT
    val cameraRelativePosition = Vector3()
    val cameraRelativeLookAtPosition = Vector3()
    val cameraRelativeTargetPosition = Vector3()
    val cameraRelativeTargetLookAtPosition = Vector3()
    val camera: PerspectiveCamera = SharedUtils.createCamera(SharedUtils.GAME_VIEW_FOV)
    lateinit var particleSystem: ParticleSystem
    lateinit var floorModel: Model
    val billboardParticleBatch: BillboardParticleBatch by lazy {
        BillboardParticleBatch(AlignMode.ViewPoint, false, 100)
    }

    override fun dispose() {
        floorModel.dispose()
    }


}
