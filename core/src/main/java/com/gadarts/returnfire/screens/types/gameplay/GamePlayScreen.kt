package com.gadarts.returnfire.screens.types.gameplay

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.ai.msg.Telegraph
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.ecs.systems.EntityBuilderImpl
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.ProfilingSystem
import com.gadarts.returnfire.ecs.systems.ai.AiSystemImpl
import com.gadarts.returnfire.ecs.systems.ai.logic.path.MapPathFinder
import com.gadarts.returnfire.ecs.systems.ai.logic.path.PathHeuristic
import com.gadarts.returnfire.ecs.systems.bullet.BulletSystem
import com.gadarts.returnfire.ecs.systems.camera.CameraSystem
import com.gadarts.returnfire.ecs.systems.character.CharacterSystemImpl
import com.gadarts.returnfire.ecs.systems.character.factories.OpponentCharacterFactory
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.data.StainsHandler
import com.gadarts.returnfire.ecs.systems.data.pools.RigidBodyFactory
import com.gadarts.returnfire.ecs.systems.effects.EffectsSystem
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.RemoveComponentEventData
import com.gadarts.returnfire.ecs.systems.hud.HudSystemImpl
import com.gadarts.returnfire.ecs.systems.map.MapSystemImpl
import com.gadarts.returnfire.ecs.systems.physics.PhysicsSystem
import com.gadarts.returnfire.ecs.systems.player.PlayerSystemImpl
import com.gadarts.returnfire.ecs.systems.render.RenderSystem
import com.gadarts.returnfire.factories.*
import com.gadarts.returnfire.managers.EcsManager
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.managers.GeneralManagers
import com.gadarts.shared.assets.map.GameMap
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

class GamePlayScreen(
    runsOnMobile: Boolean,
    fpsTarget: Int,
    private val generalManagers: GeneralManagers,
) : Screen, Telegraph {

    init {
        val fileName = GameDebugSettings.MAP.getPaths()[0]
        generalManagers.assetsManager.load(
            fileName,
            GameMap::class.java
        )
        generalManagers.assetsManager.finishLoading()
    }

    private var initialized: Boolean = false
    private var autoAim: Boolean = true
    private lateinit var selectedCharacter: CharacterDefinition
    private val entitiesToRemove = mutableListOf<Entity>()
    private val componentsToRemove = mutableListOf<Component>()
    private val entitiesToRemoveComponentsFrom = mutableListOf<Entity>()
    private lateinit var factories: Factories
    private var pauseTime: Long = 0
    private val gameSessionData: GameSessionData by lazy {
        GameSessionData(
            runsOnMobile,
            fpsTarget,
            selectedCharacter,
            autoAim,
            generalManagers.assetsManager,
            ConsoleImpl(generalManagers.assetsManager, generalManagers.dispatcher),
            engine
        )
    }
    private val engine: PooledEngine by lazy { PooledEngine() }
    private lateinit var systems: List<GameEntitySystem>

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
                generalManagers.soundManager,
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
            HudSystemImpl(gamePlayManagers),
            ProfilingSystem(gamePlayManagers),
            MapSystemImpl(gamePlayManagers),
            AiSystemImpl(gamePlayManagers),
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
        entitiesToRemoveComponentsFrom.forEachIndexed { index, entity ->
            entity.remove(componentsToRemove[index]::class.java)
        }
        entitiesToRemove.clear()
        entitiesToRemoveComponentsFrom.clear()
        componentsToRemove.clear()
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
        pauseTime = TimeUtils.millis()
    }

    override fun dispose() {
        gameSessionData.finishSession()
        engine.systems.forEach { (it as GameEntitySystem).dispose() }
        factories.dispose()
        generalManagers.dispatcher.clear()
    }

    override fun show() {

    }

    override fun handleMessage(msg: Telegram?): Boolean {
        if (msg == null) return false

        val message = msg.message
        if (message == SystemEvents.REMOVE_ENTITY.ordinal) {
            entitiesToRemove.add(msg.extraInfo as Entity)
        } else if (message == SystemEvents.REMOVE_COMPONENT.ordinal) {
            entitiesToRemoveComponentsFrom.add(RemoveComponentEventData.entity)
            componentsToRemove.add(RemoveComponentEventData.component)
        }

        return true
    }

    fun initialize(selectedCharacter: CharacterDefinition, autoAim: Boolean) {
        this.selectedCharacter = selectedCharacter
        this.autoAim = autoAim
        gameSessionData.selectedCharacter = selectedCharacter
        if (!initialized) {
            val entityBuilderImpl = EntityBuilderImpl()
            val ecs = EcsManager(engine, entityBuilderImpl)
            createFactories(entityBuilderImpl, ecs)
            generalManagers.soundManager.sessionInitialize(gameSessionData.renderData.camera)
            entityBuilderImpl.init(engine, factories, generalManagers.dispatcher)
            val gamePlayManagers = GamePlayManagers(
                generalManagers.soundManager,
                generalManagers.assetsManager,
                generalManagers.dispatcher,
                factories,
                generalManagers.screensManagers,
                ecs,
                StainsHandler(generalManagers.assetsManager),
                MapPathFinder(gameSessionData.mapData, PathHeuristic()),
            )
            generalManagers.dispatcher.addListener(this, SystemEvents.REMOVE_ENTITY.ordinal)
            generalManagers.dispatcher.addListener(this, SystemEvents.REMOVE_COMPONENT.ordinal)
            initializeSystems(gamePlayManagers)
            initialized = true
        }
    }


}
