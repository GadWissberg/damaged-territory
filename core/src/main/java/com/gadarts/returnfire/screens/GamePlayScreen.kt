package com.gadarts.returnfire.screens

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.factories.Factories
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.managers.GeneralManagers
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.*
import com.gadarts.returnfire.systems.ai.AiSystem
import com.gadarts.returnfire.systems.bullet.BulletSystem
import com.gadarts.returnfire.systems.character.CharacterSystemImpl
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory
import com.gadarts.returnfire.systems.hud.HudSystem
import com.gadarts.returnfire.systems.map.MapSystem
import com.gadarts.returnfire.systems.physics.PhysicsSystem
import com.gadarts.returnfire.systems.player.PlayerSystemImpl
import com.gadarts.returnfire.systems.render.RenderSystem

class GamePlayScreen(
    runsOnMobile: Boolean,
    fpsTarget: Int,
    private val generalManagers: GeneralManagers,
    private val selected: CharacterDefinition,
) : Screen {

    init {
        val fileName = MapDefinition.MAP_0.getPaths()[0]
        generalManagers.assetsManager.load(
            fileName,
            GameMap::class.java
        )
        generalManagers.assetsManager.finishLoading()
    }

    private var pauseTime: Long = 0
    private val gameSessionData: GameSessionData by lazy {
        GameSessionData(
            generalManagers.assetsManager,
            runsOnMobile,
            fpsTarget,
            ConsoleImpl(generalManagers.assetsManager, generalManagers.dispatcher),
            selected
        )
    }
    private val engine: PooledEngine by lazy { PooledEngine() }
    private lateinit var systems: List<GameEntitySystem>

    override fun show() {
        val entityBuilderImpl = EntityBuilderImpl()
        val factories = Factories(
            RigidBodyFactory(),
            SpecialEffectsFactory(
                gameSessionData,
                generalManagers.soundPlayer,
                generalManagers.assetsManager,
                entityBuilderImpl
            ),
            GameModelInstanceFactory(generalManagers.assetsManager)
        )
        entityBuilderImpl.init(engine, factories, generalManagers.dispatcher)
        val gamePlayManagers = GamePlayManagers(
            engine,
            generalManagers.soundPlayer,
            generalManagers.assetsManager,
            generalManagers.dispatcher,
            factories,
            generalManagers.screensManagers,
            entityBuilderImpl
        )
        systems = listOf(
            PhysicsSystem(gamePlayManagers),
            CharacterSystemImpl(gamePlayManagers),
            ParticleEffectsSystem(gamePlayManagers),
            PlayerSystemImpl(gamePlayManagers),
            RenderSystem(gamePlayManagers),
            CameraSystem(gamePlayManagers),
            HudSystem(gamePlayManagers),
            ProfilingSystem(gamePlayManagers),
            MapSystem(gamePlayManagers),
            AiSystem(gamePlayManagers),
            BulletSystem(gamePlayManagers),
        )
        systems.forEach {
            engine.addSystem(it)
        }
        systems.forEach { system ->
            system.addListener()
        }
        engine.systems.forEach {
            (it as GameEntitySystem).initialize(
                gameSessionData, gamePlayManagers
            )
        }
        engine.systems.forEach {
            (it as GameEntitySystem).onSystemReady()
        }
    }

    override fun render(delta: Float) {
        engine.update(delta)
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            generalManagers.screensManagers.goToHangarScreen()
        }
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
        generalManagers.assetsManager.unload(MapDefinition.MAP_0.getPaths()[0])
    }

    override fun dispose() {
        gameSessionData.finishSession()
        engine.systems.forEach { (it as GameEntitySystem).dispose() }
    }


}
