package com.gadarts.returnfire.screens

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.*
import com.gadarts.returnfire.systems.bullet.BulletSystem
import com.gadarts.returnfire.systems.character.CharacterSystemImpl
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory
import com.gadarts.returnfire.systems.enemy.EnemySystem
import com.gadarts.returnfire.systems.hud.HudSystem
import com.gadarts.returnfire.systems.map.MapSystem
import com.gadarts.returnfire.systems.physics.PhysicsSystem
import com.gadarts.returnfire.systems.player.PlayerSystemImpl
import com.gadarts.returnfire.systems.render.RenderSystem

class GamePlayScreen(
    private val assetsManager: GameAssetManager,
    private val rigidBodyFactory: RigidBodyFactory,
    private val soundPlayer: SoundPlayer,
    private val runsOnMobile: Boolean,
    private val fpsTarget: Int,
    characterDefinition: CharacterDefinition,
    private val screensManager: ScreensManager
) : Screen {

    init {
        SELECTED_VEHICLE = characterDefinition
        val fileName = MapDefinition.MAP_0.getPaths()[0]
        assetsManager.load(
            fileName,
            GameMap::class.java
        )
        assetsManager.finishLoading()
    }

    private var pauseTime: Long = 0
    private val gameSessionData: GameSessionData by lazy {
        GameSessionData(
            assetsManager,
            rigidBodyFactory,
            runsOnMobile,
            fpsTarget
        )
    }
    private val engine: PooledEngine by lazy { PooledEngine() }
    private val systems: List<GameEntitySystem> = listOf(
        PhysicsSystem(),
        CharacterSystemImpl(),
        ParticleEffectsSystem(),
        PlayerSystemImpl(),
        RenderSystem(),
        CameraSystem(),
        HudSystem(),
        ProfilingSystem(),
        MapSystem(),
        EnemySystem(),
        BulletSystem(),
    )

    override fun show() {
        val dispatcher = MessageDispatcher()
        systems.forEach {
            engine.addSystem(it)
        }
        val managers = Managers(
            engine,
            soundPlayer,
            assetsManager,
            dispatcher,
            RigidBodyFactory(),
            SpecialEffectsGenerator(gameSessionData, soundPlayer, assetsManager),
            screensManager
        )
        EntityBuilder.initialize(managers.engine)
        engine.systems.forEach {
            (it as GameEntitySystem).initialize(
                gameSessionData, managers
            )
        }
        systems.forEach { system ->
            system.addListener()
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
        assetsManager.unload(MapDefinition.MAP_0.getPaths()[0])
    }

    override fun dispose() {
        engine.systems.forEach { (it as GameEntitySystem).dispose() }
        gameSessionData.dispose()
    }

    companion object {
        lateinit var SELECTED_VEHICLE: CharacterDefinition
    }


}
