package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal.newDecal
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.CharactersDefinitions
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionData.Companion.SPARK_HEIGHT_BIAS
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.player.movement.PlayerMovementHandler
import com.gadarts.returnfire.systems.player.movement.PlayerMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.movement.PlayerMovementHandlerMobile
import com.gadarts.returnfire.systems.player.react.*

class PlayerSystemImpl : GameEntitySystem(), PlayerSystem, InputProcessor {

    private val playerShootingHandler = PlayerShootingHandler()

    private val playerMovementHandler: PlayerMovementHandler by lazy {
        if (gameSessionData.runsOnMobile) PlayerMovementHandlerMobile() else PlayerMovementHandlerDesktop()
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.WEAPON_BUTTON_PRIMARY_PRESSED to PlayerSystemOnWeaponButtonPrimaryPressed(
            playerShootingHandler
        ),
        SystemEvents.WEAPON_BUTTON_PRIMARY_RELEASED to PlayerSystemOnWeaponButtonPrimaryReleased(
            playerShootingHandler
        ),
        SystemEvents.WEAPON_BUTTON_SECONDARY_PRESSED to PlayerSystemOnWeaponButtonSecondaryPressed(
            playerShootingHandler
        ),
        SystemEvents.WEAPON_BUTTON_SECONDARY_RELEASED to PlayerSystemOnWeaponButtonSecondaryReleased(
            playerShootingHandler
        ),
        SystemEvents.PHYSICS_SYSTEM_READY to PlayerSystemOnPhysicsSystemReady()
    )

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        addPlayer()
    }

    override fun resume(delta: Long) {

    }


    override fun dispose() {

    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        playerMovementHandler.update(
            gameSessionData.player,
        )
        playerShootingHandler.update()
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP -> {
                playerMovementHandler.thrust(gameSessionData.player)
            }

            Input.Keys.DOWN -> {
                playerMovementHandler.reverse(gameSessionData.player)
            }

            Input.Keys.LEFT -> {
                playerMovementHandler.applyRotation(1)
            }

            Input.Keys.RIGHT -> {
                playerMovementHandler.applyRotation(-1)
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.startPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                playerShootingHandler.startSecondaryShooting()
            }
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                playerMovementHandler.onTouchUp(keycode)
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.stopPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                playerShootingHandler.stopSecondaryShooting()
            }
        }
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    private fun addPlayer(): Entity {
        EntityBuilder.initialize(managers.engine)
        val map = managers.assetsManager.getAssetByDefinition(MapDefinition.MAP_0)
        val placedPlayer =
            map.placedElements.find { placedElement -> placedElement.definition == CharactersDefinitions.PLAYER }
        val player = createPlayer(placedPlayer!!)
        engine.addEntity(player)
        gameSessionData.player = player
        ComponentsMapper.modelInstance.get(player).hidden = GameDebugSettings.HIDE_PLAYER
        initInputMethod()
        initializePlayerHandlers()
        return player
    }

    private fun initializePlayerHandlers() {
        playerShootingHandler.initialize(
            managers.dispatcher,
            gameSessionData.player
        )
        playerMovementHandler.initialize(gameSessionData.renderData.camera)
    }

    private fun initInputMethod() {
        if (gameSessionData.runsOnMobile) {
            gameSessionData.gameSessionDataHud.touchpad.addListener(
                TouchPadListener(
                    playerMovementHandler,
                    gameSessionData.player
                )
            )
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
        }
    }

    private fun createPlayer(
        placedPlayer: PlacedElement
    ): Entity {
        val assetsManager = managers.assetsManager
        val machineGunSparkModel = assetsManager.getAssetByDefinition(ModelDefinition.MACHINE_GUN_SPARK)
        val primarySpark = addSpark(machineGunSparkModel, primaryRelativePositionCalculator)
        val secondarySpark = addSpark(machineGunSparkModel, secRelativePositionCalculator)
        val entityBuilder =
            EntityBuilder.begin()
                .addModelInstanceComponent(
                    createPlayerModelInstance(),
                    auxVector3_1.set(placedPlayer.col.toFloat(), PLAYER_HEIGHT, placedPlayer.row.toFloat()),
                    false,
                )
        if (GameDebugSettings.DISPLAY_PROPELLER) {
            addPropeller(entityBuilder)
        }
        val propellerSound = assetsManager.getAssetByDefinition(SoundDefinition.PROPELLER)
        entityBuilder.addAmbSoundComponent(
            propellerSound
        )
            .addCharacterComponent(INITIAL_HP)
            .addPlayerComponent()
        addFirepowerToPlayer(entityBuilder, primarySpark, secondarySpark)
        val player = entityBuilder.finish()
        ComponentsMapper.spark.get(primarySpark).parent = player
        ComponentsMapper.spark.get(secondarySpark).parent = player
        return player
    }

    private fun addFirepowerToPlayer(entityBuilder: EntityBuilder, primarySpark: Entity, secondarySpark: Entity) {
        addPrimaryArmComponent(entityBuilder, primarySpark)
        addSecondaryArmComponent(entityBuilder, secondarySpark)
    }

    private fun createPlayerModelInstance(): GameModelInstance {
        val apacheModel = managers.assetsManager.getAssetByDefinition(ModelDefinition.APACHE)
        return GameModelInstance(
            ModelInstance(apacheModel),
            ModelDefinition.APACHE,
            managers.assetsManager.getCachedBoundingBox(ModelDefinition.APACHE),
        )
    }

    private val primaryRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            val transform =
                ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform
            val pos = output.set(0.3F, 0F, 0F).rot(transform)
            pos.y -= SPARK_HEIGHT_BIAS
            return pos
        }
    }

    private fun addPrimaryArmComponent(
        entityBuilder: EntityBuilder,
        primarySpark: Entity,
    ): EntityBuilder {
        val assetsManager = managers.assetsManager
        entityBuilder.addPrimaryArmComponent(
            primarySpark,
            ArmProperties(
                assetsManager.getAssetByDefinition(SoundDefinition.MACHINE_GUN),
                PRI_RELOAD_DUR,
                PRI_BULLET_SPEED,
                assetsManager.getCachedBoundingBox(ModelDefinition.BULLET).width / 2F,
                ParticleEffectDefinition.SMOKE_SMALL,
                ModelDefinition.BULLET,
                null,
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
                null
            ),
        )
        return entityBuilder
    }

    private fun addSpark(
        machineGunSparkModel: Model,
        relativePositionCalculator: ArmComponent.RelativePositionCalculator
    ) =
        EntityBuilder.begin()
            .addModelInstanceComponent(
                GameModelInstance(ModelInstance(machineGunSparkModel), ModelDefinition.MACHINE_GUN_SPARK),
                Vector3(),
                true, hidden = true
            )
            .addSparkComponent(relativePositionCalculator)
            .finishAndAddToEngine()

    private val secRelativePositionCalculator = object : ArmComponent.RelativePositionCalculator {
        override fun calculate(parent: Entity, output: Vector3): Vector3 {
            playerShootingHandler.secondaryCreationSide =
                !playerShootingHandler.secondaryCreationSide
            val transform =
                ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform
            val pos =
                output.set(
                    0.5F,
                    0F,
                    if (playerShootingHandler.secondaryCreationSide) 1F else -1F
                )
                    .rot(transform).scl(SECONDARY_POSITION_BIAS)
            pos.y -= SPARK_HEIGHT_BIAS
            return pos
        }
    }

    private fun addSecondaryArmComponent(
        entityBuilder: EntityBuilder,
        secondarySpark: Entity,
    ): EntityBuilder {
        entityBuilder.addSecondaryArmComponent(
            secondarySpark,
            ArmProperties(
                managers.assetsManager.getAssetByDefinition(SoundDefinition.MISSILE),
                SEC_RELOAD_DUR,
                SEC_BULLET_SPEED,
                managers.assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE).width,
                ParticleEffectDefinition.EXPLOSION_SMALL,
                ModelDefinition.MISSILE,
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_EMIT),
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_SMALL),
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL_LOOP)
            ),
        )
        return entityBuilder
    }

    private fun addPropeller(
        entityBuilder: EntityBuilder
    ) {
        val definitions = managers.assetsManager.getTexturesDefinitions()
        val propTextureRegion =
            TextureRegion(managers.assetsManager.getTexture(definitions.definitions["propeller_blurred"]!!))
        val propDec = newDecal(PROP_SIZE, PROP_SIZE, propTextureRegion, true)
        propDec.rotateX(90F)
        val decals = listOf(ChildDecal(propDec, Vector3.Zero))
        entityBuilder.addChildDecalComponent(decals, true)
    }


    companion object {
        private const val INITIAL_HP = 100
        private val auxVector3_1 = Vector3()
        private const val PRI_RELOAD_DUR = 125L
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 1.5F
        private const val PRI_BULLET_SPEED = 16F
        private const val SEC_BULLET_SPEED = 5F
        private const val SECONDARY_POSITION_BIAS = 0.2F
        private const val PLAYER_HEIGHT = 3.9F
    }

}
