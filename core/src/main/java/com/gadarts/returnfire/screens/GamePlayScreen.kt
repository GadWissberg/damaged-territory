package com.gadarts.returnfire.screens

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.factories.*
import com.gadarts.returnfire.managers.EcsManager
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.managers.GeneralManagers
import com.gadarts.returnfire.managers.PathHeuristic
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.systems.*
import com.gadarts.returnfire.systems.ai.AiSystem
import com.gadarts.returnfire.systems.ai.MapPathFinder
import com.gadarts.returnfire.systems.bullet.BulletSystem
import com.gadarts.returnfire.systems.character.CharacterSystemImpl
import com.gadarts.returnfire.systems.character.factories.OpponentCharacterFactory
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.StainsHandler
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.hud.HudSystem
import com.gadarts.returnfire.systems.map.MapSystemImpl
import com.gadarts.returnfire.systems.physics.PhysicsSystem
import com.gadarts.returnfire.systems.player.PlayerSystemImpl
import com.gadarts.returnfire.systems.render.RenderSystem

class GamePlayScreen(
    runsOnMobile: Boolean,
    fpsTarget: Int,
    private val generalManagers: GeneralManagers,
    private val selected: CharacterDefinition,
    autoAim: Boolean,
) : Screen, Telegraph {

    init {
        val fileName = GameDebugSettings.MAP.getPaths()[0]
        generalManagers.assetsManager.load(
            fileName,
            GameMap::class.java
        )
        generalManagers.assetsManager.finishLoading()
    }

    private val entitiesToRemove = mutableListOf<Entity>()
    private lateinit var factories: Factories
    private var pauseTime: Long = 0
    private val gameSessionData: GameSessionData by lazy {
        GameSessionData(
            generalManagers.assetsManager,
            runsOnMobile,
            fpsTarget,
            ConsoleImpl(generalManagers.assetsManager, generalManagers.dispatcher),
            selected,
            autoAim
        )
    }
    private val engine: PooledEngine by lazy { PooledEngine() }
    private lateinit var systems: List<GameEntitySystem>

    override fun show() {
        val entityBuilderImpl = EntityBuilderImpl()
        val ecs = EcsManager(engine, entityBuilderImpl)
        createFactories(entityBuilderImpl, ecs)
        generalManagers.soundPlayer.sessionInitialize(gameSessionData.renderData.camera)
        entityBuilderImpl.init(engine, factories, generalManagers.dispatcher)
        val gamePlayManagers = GamePlayManagers(
            generalManagers.soundPlayer,
            generalManagers.assetsManager,
            generalManagers.dispatcher,
            factories,
            generalManagers.screensManagers,
            ecs,
            StainsHandler(generalManagers.assetsManager),
            MapPathFinder(gameSessionData.mapData, PathHeuristic()),
        )
        generalManagers.dispatcher.addListener(this, SystemEvents.REMOVE_ENTITY.ordinal)
        initializeSystems(gamePlayManagers)
    }

    private fun createFactories(
        entityBuilderImpl: EntityBuilderImpl,
        ecs: EcsManager
    ) {
        val gameModelInstanceFactory = GameModelInstanceFactory(generalManagers.assetsManager)
        val opponentCharacterFactory =
            OpponentCharacterFactory(
                generalManagers.assetsManager,
                gameSessionData,
                gameModelInstanceFactory,
                entityBuilderImpl,
            )
        factories = Factories(
            RigidBodyFactory(),
            GhostFactory(),
            SpecialEffectsFactory(
                gameSessionData,
                generalManagers.soundPlayer,
                generalManagers.assetsManager,
                entityBuilderImpl,
                ecs,
                gameModelInstanceFactory
            ),
            gameModelInstanceFactory,
            AutoAimShapeFactory(gameSessionData),
            opponentCharacterFactory
        )
    }

    private fun initializeSystems(gamePlayManagers: GamePlayManagers) {
        systems = listOf(
            PhysicsSystem(gamePlayManagers),
            CharacterSystemImpl(gamePlayManagers),
            EffectsSystem(gamePlayManagers),
            PlayerSystemImpl(gamePlayManagers),
            RenderSystem(gamePlayManagers),
            CameraSystem(gamePlayManagers),
            HudSystem(gamePlayManagers),
            ProfilingSystem(gamePlayManagers),
            MapSystemImpl(gamePlayManagers),
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
        entitiesToRemove.forEach {
            engine.removeEntity(it)
        }
        entitiesToRemove.clear()
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
        generalManagers.assetsManager.unload(GameDebugSettings.MAP.getPaths()[0])
    }

    override fun dispose() {
        gameSessionData.finishSession()
        engine.systems.forEach { (it as GameEntitySystem).dispose() }
        factories.dispose()
    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        entitiesToRemove.add(msg.extraInfo as Entity)

        return true
    }


}
