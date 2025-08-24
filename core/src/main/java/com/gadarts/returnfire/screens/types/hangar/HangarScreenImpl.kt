package com.gadarts.returnfire.screens.types.hangar

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.scenes.scene2d.Stage
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.shared.GameAssetManager


class HangarScreenImpl(
    dispatcher: MessageDispatcher,
    runsOnMobile: Boolean,
    assetsManager: GameAssetManager,
    screenManager: ScreensManager,
    soundManager: SoundManager,
) : HangarScreen {
    private val stage = Stage()
    private val hangarSceneHandler = HangarSceneHandler(soundManager, assetsManager, screenManager)
    private val hangarScreenMenu = HangarScreenMenu(runsOnMobile, assetsManager, stage, hangarSceneHandler)
    private var initialized: Boolean = false

    private val console = ConsoleImpl(assetsManager, dispatcher)

    override fun show() {
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(stage)
        if (initialized) {
            hangarSceneHandler.returnFromCombat()
        } else {
            initialize()
        }
    }


    private fun initialize() {
        initialized = true
        hangarSceneHandler.init(hangarScreenMenu)
        hangarScreenMenu.init()
        stage.addActor(console)
        console.toFront()
    }


    override fun render(delta: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.BACK) || Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }
        hangarSceneHandler.render(delta)
        stage.act()
        stage.draw()
    }


    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
        inputMultiplexer.removeProcessor(console)
        inputMultiplexer.removeProcessor(stage)
    }

    override fun dispose() {
        hangarSceneHandler.dispose()
        stage.dispose()
    }

}
