package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.scene.AuxiliaryModels
import com.gadarts.shared.GameAssetManager

data class SceneRendererHandlers(
    private val auxiliaryModels: AuxiliaryModels,
    private val assetsManager: GameAssetManager,
) : Disposable {
    private val sharedData = SceneRendererHandlersSharedData()
    val handlers = listOf<SceneHandler>(
        CameraHandler(sharedData),
        RenderingHandler(auxiliaryModels, sharedData),
        CursorHandler(sharedData)
    )

    override fun dispose() {
        handlers.forEach { it.dispose() }
        auxiliaryModels.dispose()
    }

    fun initialize(tiles: Array<Array<ModelInstance>>) {
        tiles.forEach { tilesRow ->
            tilesRow.forEach { tile ->
                sharedData.modelInstances.add(tile)
            }
        }
    }

    fun update(parent: Table) {
        val deltaTime = Gdx.graphics.deltaTime
        handlers.forEach { handler ->
            handler.update(parent, deltaTime)
        }
    }


}
