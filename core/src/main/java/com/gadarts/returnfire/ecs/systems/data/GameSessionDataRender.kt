package com.gadarts.returnfire.ecs.systems.data

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA
import com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.particles.ParticleShader.AlignMode
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem
import com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.systems.camera.CameraState
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
    val haloModel: Model by lazy {
        val modelBuilder = ModelBuilder()
        modelBuilder.createSphere(
            0.5F, 1F, 0.5F, 16, 16,
            Material(
                ColorAttribute.createDiffuse(Color.WHITE),
                BlendingAttribute(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 0.1F)
            ),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()
        )
    }


    override fun dispose() {
        floorModel.dispose()
        haloModel.dispose()
    }


}
