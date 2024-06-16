package com.gadarts.returnfire

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.systems.*
import com.gadarts.returnfire.systems.bullet.BulletSystem
import com.gadarts.returnfire.systems.character.CharacterSystemImpl
import com.gadarts.returnfire.systems.hud.HudSystem
import com.gadarts.returnfire.systems.map.MapSystem
import com.gadarts.returnfire.systems.player.PlayerSystemImpl
import com.gadarts.returnfire.systems.render.RenderSystem

class GamePlayScreen(
    private val assetsManager: GameAssetManager,
    private val soundPlayer: SoundPlayer,
    private val runsOnMobile: Boolean
) : Screen {


    private var pauseTime: Long = 0
    private lateinit var gameSessionData: GameSessionData
    private lateinit var engine: PooledEngine
    private val systems: List<GameEntitySystem> = listOf(
        CharacterSystemImpl(),
        PlayerSystemImpl(),
        RenderSystem(),
        CameraSystem(),
        HudSystem(),
        ProfilingSystem(),
        MapSystem(),
        BulletSystem(),
        ParticleEffectsSystem()
    )

    override fun show() {
        this.engine = PooledEngine()
        gameSessionData = GameSessionData(assetsManager, runsOnMobile)
        val dispatcher = MessageDispatcher()
        systems.forEach {
            engine.addSystem(it)
        }
        val managers = Managers(engine, soundPlayer, assetsManager, dispatcher)
        engine.systems.forEach {
            (it as GameEntitySystem).initialize(
                gameSessionData, managers
            )
        }
        systems.forEach { system ->
            system.addListener(
                system
            )
        }
        engine.systems.forEach {
            (it as GameEntitySystem).onSystemReady()
        }
    }

    override fun render(delta: Float) {
        engine.update(delta)
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
        pauseTime = TimeUtils.millis()
    }

    override fun resume() {
        val delta = TimeUtils.timeSinceMillis(pauseTime)
        engine.systems.forEach { (it as GameEntitySystem).resume(delta) }
    }

    override fun hide() {
    }

    override fun dispose() {
        engine.systems.forEach { (it as GameEntitySystem).dispose() }
        gameSessionData.dispose()
    }


}
