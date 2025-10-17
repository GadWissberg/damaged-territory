package com.gadarts.dte.scene.handlers.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException
import com.gadarts.dte.EditorEvents
import com.gadarts.dte.scene.AuxiliaryModels
import com.gadarts.dte.scene.Modes
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.EditorOnEvent
import com.gadarts.dte.scene.handlers.SceneHandler

class RenderingHandler(
    private val auxiliaryModels: AuxiliaryModels,
    private val sharedData: SharedData,
    dispatcher: MessageDispatcher,
) : Disposable, SceneHandler(dispatcher) {
    private val shaderProgram: ShaderProgram = ShaderProgram(
        Gdx.files.internal("shaders/gray_vertex.glsl"),
        Gdx.files.internal("shaders/gray_fragment.glsl")
    )
    private val grayShader = GrayShader(shaderProgram)

    override val subscribedEvents: Map<EditorEvents, EditorOnEvent> = mapOf(
        EditorEvents.LAYER_SELECTED to object : EditorOnEvent {
            override fun react(msg: Telegram) {
                sharedData.mapData.layers.forEachIndexed { index, tileLayer ->
                    if (index == sharedData.selectionData.selectedLayerIndex || index == 0) {
                        tileLayer.tiles.forEach { row ->
                            row.forEach { tile ->
                                tile?.modelInstance?.applyGray = false
                            }
                        }
                    } else {
                        tileLayer.tiles.forEach { row ->
                            row.forEach { tile ->
                                tile?.modelInstance?.applyGray = true
                            }
                        }
                    }
                }
            }
        },
        EditorEvents.MODE_CHANGED to object : EditorOnEvent {
            override fun react(msg: Telegram) {
                if (sharedData.selectionData.selectedMode == Modes.TILES) return

                sharedData.mapData.layers.forEachIndexed { _, tileLayer ->
                    tileLayer.tiles.forEach { row ->
                        row.forEach { tile ->
                            tile?.modelInstance?.applyGray = false
                        }
                    }
                }
            }
        }
    )

    private val modelBatch: ModelBatch by lazy { ModelBatch(EditorShaderProvider(grayShader)) }
    private val shapeRenderer = ShapeRenderer()

    private fun render(screenPosition: Vector2) {
        Gdx.gl.glViewport(
            screenPosition.x.toInt(),
            screenPosition.y.toInt(),
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        modelBatch.begin(sharedData.camera)
        auxiliaryModels.render(modelBatch)
        modelBatch.end()
        modelBatch.begin(sharedData.camera)
        sharedData.mapData.modelInstances.forEach { modelInstance ->
            if (modelInstance.applyGray) {
                renderEditorModelInstance(modelInstance)
            }
        }
        modelBatch.end()
        modelBatch.begin(sharedData.camera)
        sharedData.mapData.modelInstances.forEach { modelInstance ->
            if (!modelInstance.applyGray) {
                renderEditorModelInstance(modelInstance)
            }
        }
        modelBatch.end()
    }

    private fun renderEditorModelInstance(modelInstance: EditorModelInstance) {
        modelInstance.userData = modelInstance
        modelBatch.render(modelInstance)
        modelInstance.relatedModelsToBeRenderedInEditor?.forEach { relatedModelToBeRenderedInEditor ->
            renderEditorModelInstance(relatedModelToBeRenderedInEditor)
        }
    }

    init {
        if (!shaderProgram.isCompiled) {
            throw GdxRuntimeException("Shader compile error: " + shaderProgram.log)
        }
    }

    override fun update(parent: Table, deltaTime: Float) {
        val screenPosition = parent.localToScreenCoordinates(auxVector2.set(0F, 0F))
        render(screenPosition)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        shaderProgram.dispose()
    }

    companion object {
        private val auxVector2 = Vector2()
    }
}
