package com.gadarts.dte

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.SharedUtils

class CameraHandler {
    fun update() {
        cameraController.update()
        camera.update()
    }

    private fun createCamera(): PerspectiveCamera {
        val perspectiveCamera = SharedUtils.createCamera(SharedUtils.GAME_VIEW_FOV)
        perspectiveCamera.position[9.0f, 16.0f] = 9.0f
        perspectiveCamera.direction.rotate(Vector3.X, -55.0f)
        perspectiveCamera.direction.rotate(Vector3.Y, 45.0f)
        return perspectiveCamera
    }

    val camera: PerspectiveCamera = createCamera()
    private val cameraController = CameraInputController(camera)

}
