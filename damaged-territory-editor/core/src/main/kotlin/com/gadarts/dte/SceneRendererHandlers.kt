package com.gadarts.dte

import com.badlogic.gdx.utils.Disposable

data class SceneRendererHandlers(
    private val auxiliaryModels: AuxiliaryModels
) : Disposable {
    val cameraHandler = CameraHandler()
    val renderingHandler =
        RenderingHandler(auxiliaryModels, cameraHandler)

    override fun dispose() {
        auxiliaryModels.dispose()
    }

}
