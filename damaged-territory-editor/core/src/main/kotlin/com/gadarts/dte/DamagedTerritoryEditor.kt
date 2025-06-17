package com.gadarts.dte

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils
import com.gadarts.dte.scene.SceneRenderer
import com.gadarts.dte.scene.SharedData
import com.gadarts.dte.ui.EditorUi
import com.gadarts.shared.GameAssetManager

class DamagedTerritoryEditor(private val dispatcher: MessageDispatcher) : ApplicationAdapter() {
    private val sharedData = SharedData()
    private val gameAssetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val tileFactory = TileFactory(sharedData, gameAssetsManager)
    private val objectFactory = ObjectFactory(sharedData, gameAssetsManager)
    private val ui by lazy { EditorUi(sharedData, tileFactory, objectFactory, dispatcher, gameAssetsManager) }
    private val sceneRenderer: SceneRenderer by lazy {
        SceneRenderer(
            sharedData,
            gameAssetsManager,
            dispatcher,
            tileFactory,
            objectFactory
        )
    }

    override fun create() {
        Gdx.input.inputProcessor = InputMultiplexer()
        gameAssetsManager.loadAssets()
        sceneRenderer.initialize()
        ui.initialize()
    }

    override fun render() {
        super.render()
        Gdx.gl.glViewport(
            0,
            0,
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight
        )
        ScreenUtils.clear(Color.BLACK, true)
        sceneRenderer.render()
        ui.render()
    }

    override fun dispose() {
        super.dispose()
        sharedData.dispose()
    }
}
