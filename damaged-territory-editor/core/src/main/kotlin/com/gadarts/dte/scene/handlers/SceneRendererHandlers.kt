package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.ObjectFactory
import com.gadarts.dte.TileFactory
import com.gadarts.dte.scene.AuxiliaryModels
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.scene.handlers.render.RenderingHandler
import com.gadarts.shared.GameAssetManager

data class SceneRendererHandlers(
    val dispatcher: MessageDispatcher,
    val sharedData: SharedData,
    private val auxiliaryModels: AuxiliaryModels,
    private val assetsManager: GameAssetManager,
    private val tileFactory: TileFactory,
    private val objectFactory: ObjectFactory
) : Disposable {

    private val handlers = listOf(
        CameraHandler(sharedData, dispatcher),
        RenderingHandler(auxiliaryModels, sharedData, dispatcher),
        CursorHandler(sharedData, assetsManager, tileFactory, objectFactory, dispatcher),
        MapHandler(sharedData, assetsManager, dispatcher)
    )

    override fun dispose() {
        handlers.forEach { it.dispose() }
    }

    fun update(parent: Table) {
        val deltaTime = Gdx.graphics.deltaTime
        handlers.forEach { handler ->
            handler.update(parent, deltaTime)
        }
    }

    fun initialize() {
        handlers.forEach { handler ->
            handler.initialize()
        }
    }


}
