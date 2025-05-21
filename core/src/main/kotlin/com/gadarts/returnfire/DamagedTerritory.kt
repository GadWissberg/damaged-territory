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
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.screens.hangar.HangarScreenImpl

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game(),
    ScreensManager {
    private val dispatcher = MessageDispatcher()
    private val soundManager: SoundManager by lazy { SoundManager(assetsManager, runsOnMobile) }
    private val hangarScreenImpl by lazy {
        HangarScreenImpl(
            dispatcher,
            runsOnMobile,
            assetsManager,
            this,
            soundManager
        )
    }
    private val assetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val customDesktopLib: String =
        "C:\\Users\\gadw1\\StudioProjects\\libgdx\\extensions\\gdx-bullet\\jni\\vs\\gdxBullet\\x64\\Debug\\gdxBullet.dll"

    @Suppress(
        "KotlinConstantConditions", "SimplifyBooleanWithConstants",
        "UnsafeDynamicallyLoadedCode"
    )
    override fun create() {
        val screenWidth = Gdx.graphics.displayMode.width
        val screenHeight = Gdx.graphics.displayMode.height
        val targetWidth = (screenWidth * 0.85).toInt().coerceAtMost(MAX_RESOLUTION_WIDTH)
        val targetHeight = (screenHeight * 0.85).toInt().coerceAtMost(MAX_RESOLUTION_HEIGHT)
        Gdx.graphics.setWindowedMode(targetWidth, targetHeight)
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        assetsManager.loadAssets()
        soundManager.play(assetsManager.getAssetByDefinition(MusicDefinition.TEST))
        Gdx.input.inputProcessor = InputMultiplexer()
        if (GameDebugSettings.USE_DEBUG_DLL) {
            System.load(customDesktopLib)
        } else {
            Bullet.init()
        }
        if (GameDebugSettings.SELECTED_VEHICLE != null) {
            goToWarScreen(
                GameDebugSettings.SELECTED_VEHICLE,
                GameDebugSettings.FORCE_AIM > 0
            )
        } else {
            setScreen(hangarScreenImpl)
        }
    }

    override fun goToWarScreen(characterDefinition: CharacterDefinition, autoAim: Boolean) {
        setScreen(
            GamePlayScreen(
                runsOnMobile,
                fpsTarget,
                GeneralManagers(assetsManager, soundManager, dispatcher, this),
                characterDefinition,
                autoAim
            )
        )
    }

    override fun goToHangarScreen() {
        dispatcher.clearListeners()
        soundManager.stopAll(assetsManager)
        screen.dispose()
        setScreen(hangarScreenImpl)
    }

    companion object {
        const val VERSION: String = "0.10"
        const val MAX_RESOLUTION_WIDTH = 1920
        const val MAX_RESOLUTION_HEIGHT = 1080

    }
}
