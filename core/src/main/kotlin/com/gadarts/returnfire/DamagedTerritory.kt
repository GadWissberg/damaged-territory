package com.gadarts.returnfire

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.returnfire.screens.SelectionScreen
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game(),
    ScreensManager {
    private val assetsManager: GameAssetManager by lazy { GameAssetManager() }
    private val rigidBodyFactory = RigidBodyFactory()

    override fun create() {
        Gdx.graphics.setWindowedMode(1920, 1080)
        loadAssets()
        Gdx.input.inputProcessor = InputMultiplexer()
        setScreen(SelectionScreen(runsOnMobile, assetsManager, this))
    }


    private fun loadAssets() {
        assetsManager.loadAssets()
    }

    override fun goToWarScreen(characterDefinition: CharacterDefinition) {
        val soundPlayer = SoundPlayer()
        setScreen(
            GamePlayScreen(
                assetsManager,
                rigidBodyFactory,
                soundPlayer,
                runsOnMobile,
                fpsTarget,
                characterDefinition
            )
        )

    }

}
