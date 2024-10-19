package com.gadarts.returnfire

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.screens.SelectionScreen
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game(),
    ScreensManager {
    private val soundPlayer: SoundPlayer by lazy { SoundPlayer() }
    private val selectionScreen by lazy { SelectionScreen(assetsManager, this, runsOnMobile) }
    private val assetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val rigidBodyFactory = RigidBodyFactory()

    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Gdx.graphics.setWindowedMode(1920, 1080)
        assetsManager.loadAssets()
        Gdx.input.inputProcessor = InputMultiplexer()
        setScreen(selectionScreen)
    }

    override fun goToWarScreen(characterDefinition: CharacterDefinition) {
        setScreen(
            GamePlayScreen(
                assetsManager,
                rigidBodyFactory,
                soundPlayer,
                runsOnMobile,
                fpsTarget,
                characterDefinition,
                this
            )
        )

    }

    override fun goToSelectionScreen() {
        soundPlayer.stopAll(assetsManager)
        screen.dispose()
        setScreen(selectionScreen)
    }

}
