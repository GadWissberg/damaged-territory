package com.gadarts.returnfire

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.physics.bullet.Bullet
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.OpponentEnteredGameplayScreenEventData
import com.gadarts.returnfire.managers.GeneralManagers
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.screens.ScreenSwitchParameters
import com.gadarts.returnfire.screens.Screens
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.screens.transition.TransitionHandler
import com.gadarts.returnfire.screens.types.gameplay.GamePlayScreen
import com.gadarts.returnfire.screens.types.gameplay.GamePlayScreenSwitchParameters
import com.gadarts.returnfire.screens.types.hangar.HangarScreenImpl
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.MusicDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.CharacterDefinition

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game(),
    ScreensManager {
    private val gamePlayScreen: GamePlayScreen by lazy {
        val screen = GamePlayScreen(
            runsOnMobile,
            fpsTarget,
            GeneralManagers(assetsManager, soundManager, dispatcher, this),
        )
        screen
    }
    private val transitionHandler = TransitionHandler(this)
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
        "KotlinConstantConditions",
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
            goToGameplayScreen(
                GameDebugSettings.SELECTED_VEHICLE,
                GameDebugSettings.FORCE_AIM > 0
            )
        } else {
            setScreen(hangarScreenImpl)
        }
    }

    override fun render() {
        super.render()
        transitionHandler.render(Gdx.graphics.deltaTime)
    }

    override fun setScreenWithFade(screen: Screens, durationInSeconds: Float, param: ScreenSwitchParameters?) {
        transitionHandler.switch(screen, durationInSeconds, param)
    }

    override fun switchScreen(screen: Screens, param: ScreenSwitchParameters?) {
        if (screen == Screens.VEHICLE_SELECTION) {
            goToHangarScreen()
        } else {
            val parameters = param as GamePlayScreenSwitchParameters
            goToGameplayScreen(parameters.selectedVehicle, parameters.autoAim)
        }
    }

    override fun goToGameplayScreen(selectedCharacter: CharacterDefinition, autoAim: Boolean) {
        setScreen(
            gamePlayScreen
        )
        gamePlayScreen.initialize(selectedCharacter, autoAim)
        OpponentEnteredGameplayScreenEventData.set(
            CharacterColor.BROWN,
            selectedCharacter
        )
        dispatcher.dispatchMessage(SystemEvents.OPPONENT_ENTERED_GAME_PLAY_SCREEN.ordinal, CharacterColor.BROWN)
    }

    override fun goToHangarScreen() {
        soundManager.stopAll(assetsManager)
        setScreen(hangarScreenImpl)
    }

    companion object {
        const val VERSION: String = "0.10"
        const val MAX_RESOLUTION_WIDTH = 1920
        const val MAX_RESOLUTION_HEIGHT = 1080

    }
}
