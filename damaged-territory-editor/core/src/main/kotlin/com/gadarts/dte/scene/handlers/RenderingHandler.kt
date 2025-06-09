package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.scene.AuxiliaryModels
import com.gadarts.dte.scene.SharedData

class RenderingHandler(
    private val auxiliaryModels: AuxiliaryModels,
    private val sharedData: SharedData,
    dispatcher: MessageDispatcher,
) : Disposable, SceneHandler(dispatcher) {
    private val modelsBatch = ModelBatch()
    private val shapeRenderer = ShapeRenderer()
    private fun render(screenPosition: Vector2) {
        Gdx.gl.glViewport(
            screenPosition.x.toInt(),
            screenPosition.y.toInt(),
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        modelsBatch.begin(sharedData.camera)
        auxiliaryModels.render(modelsBatch)
        sharedData.modelInstances.forEach { modelInstance ->
            modelsBatch.render(modelInstance)
        }
        modelsBatch.end()
    }

    override fun update(parent: Table, deltaTime: Float) {
        val screenPosition = parent.localToScreenCoordinates(auxVector2.set(0F, 0F))
        render(screenPosition)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }

    companion object {
        private val auxVector2 = Vector2()
    }
}
