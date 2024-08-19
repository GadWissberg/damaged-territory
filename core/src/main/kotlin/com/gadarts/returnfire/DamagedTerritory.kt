package com.gadarts.returnfire

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.gadarts.returnfire.assets.GameAssetManager

class DamagedTerritory(private val runsOnMobile: Boolean, private val fpsTarget: Int) : Game() {
    private lateinit var assetsManager: GameAssetManager

    override fun create() {
        Gdx.graphics.setWindowedMode(1920, 1080)
        loadAssets()
        Gdx.input.inputProcessor = InputMultiplexer()
        val soundPlayer = SoundPlayer()
        setScreen(GamePlayScreen(assetsManager, soundPlayer, runsOnMobile, fpsTarget))
    }


    private fun loadAssets() {
        assetsManager = GameAssetManager()
        assetsManager.loadAssets()
    }

}
