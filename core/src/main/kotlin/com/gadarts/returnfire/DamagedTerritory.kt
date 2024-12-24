package com.gadarts.returnfire

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.physics.bullet.Bullet
import com.gadarts.returnfire.assets.definitions.MusicDefinition
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.GeneralManagers
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.screens.HangarScreen
import com.gadarts.returnfire.screens.ScreensManager

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game(),
    ScreensManager {
    private val dispatcher = MessageDispatcher()
    private val soundPlayer: SoundPlayer by lazy { SoundPlayer() }
    private val hangarScreen by lazy { HangarScreen(assetsManager, dispatcher, this, soundPlayer, runsOnMobile) }
    private val assetsManager: GameAssetManager by lazy { GameAssetManager() }

    @Suppress("SENSELESS_COMPARISON")
    override fun create() {
        val screenWidth = Gdx.graphics.displayMode.width
        val screenHeight = Gdx.graphics.displayMode.height
        val targetWidth = (screenWidth * 0.85).toInt().coerceAtMost(MAX_RESOLUTION_WIDTH)
        val targetHeight = (screenHeight * 0.85).toInt().coerceAtMost(MAX_RESOLUTION_HEIGHT)
        Gdx.graphics.setWindowedMode(targetWidth, targetHeight)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        assetsManager.loadAssets()
        soundPlayer.play(assetsManager.getAssetByDefinition(MusicDefinition.TEST))
        Gdx.input.inputProcessor = InputMultiplexer()
        Bullet.init()
        if (GameDebugSettings.SELECTED_VEHICLE != null) {
            goToWarScreen(
                GameDebugSettings.SELECTED_VEHICLE,
                GameDebugSettings.FORCE_AIM > 0
            )
        } else {
            setScreen(hangarScreen)
        }
    }

    override fun goToWarScreen(characterDefinition: CharacterDefinition, autoAim: Boolean) {
        setScreen(
            GamePlayScreen(
                runsOnMobile,
                fpsTarget,
                GeneralManagers(assetsManager, soundPlayer, dispatcher, this),
                characterDefinition,
                autoAim
            )
        )
    }

    override fun goToHangarScreen() {
        dispatcher.clearListeners()
        soundPlayer.stopAll(assetsManager)
        screen.dispose()
        setScreen(hangarScreen)
    }

    companion object {
        const val VERSION: String = "0.8"
        private const val MAX_RESOLUTION_WIDTH = 1920
        private const val MAX_RESOLUTION_HEIGHT = 1080

    }
}
