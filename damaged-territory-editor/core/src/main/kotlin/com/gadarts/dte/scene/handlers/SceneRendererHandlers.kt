package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.scene.AuxiliaryModels
import com.gadarts.shared.GameAssetManager

data class SceneRendererHandlers(
    private val auxiliaryModels: AuxiliaryModels,
    private val assetsManager: GameAssetManager,
) : Disposable {
    val cameraHandler = CameraHandler()
    val renderingHandler =
        RenderingHandler(auxiliaryModels, cameraHandler)

    override fun dispose() {
        auxiliaryModels.dispose()
    }

    fun initialize(tiles: Array<Array<ModelInstance>>) {
        renderingHandler.initialize(tiles)
    }

}
