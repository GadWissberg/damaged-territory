package com.gadarts.shared

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera

object SharedUtils {
    const val GAME_VIEW_FOV = 60F

    fun createCamera(fov: Float): PerspectiveCamera {
        val perspectiveCamera = PerspectiveCamera(
            fov,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        perspectiveCamera.near = 0.1F
        perspectiveCamera.far = 300F
        return perspectiveCamera
    }

}
