package com.gadarts.dte.scene

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.gadarts.dte.ObjectFactory
import com.gadarts.dte.TileFactory
import com.gadarts.dte.scene.handlers.SceneRendererHandlers
import com.gadarts.shared.GameAssetManager

class SceneRenderer(
    sharedData: SharedData,
    assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher,
    tileFactory: TileFactory,
    objectFactory: ObjectFactory
) : Table(),
    Disposable {
    private val auxiliaryModels = AuxiliaryModels(MAP_SIZE)
    private val handlers =
        SceneRendererHandlers(dispatcher, sharedData, auxiliaryModels, assetsManager, tileFactory, objectFactory)

    fun render() {
        handlers.update(this)
    }


    override fun dispose() {
        handlers.dispose()
        auxiliaryModels.dispose()
    }

    fun initialize() {
        handlers.initialize()
    }


    companion object {
        const val MAP_SIZE = 32
    }
}
