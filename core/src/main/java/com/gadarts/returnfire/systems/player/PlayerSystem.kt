package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.decals.Decal.newDecal
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.ModelsDefinitions
import com.gadarts.returnfire.assets.SfxDefinitions
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.systems.*
import com.gadarts.returnfire.systems.GameSessionData.Companion.SPARK_HEIGHT_BIAS
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryReleased
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryReleased

class PlayerSystem : GameEntitySystem(), InputProcessor {

    private val playerShootingHandler = PlayerShootingHandler()
    private val playerMovementHandler = PlayerMovementHandler()
    private lateinit var propellerBlurredModel: Model
    private lateinit var player: Entity

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        createPropellerBlurredModel(services.assetsManager)
        player = addPlayer(engine as PooledEngine, services.assetsManager)
        gameSessionData.player = player
        playerShootingHandler.initialize(services.assetsManager)
        playerMovementHandler.initialize(engine, services.assetsManager, gameSessionData.camera, gameSessionData.player)
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
    }

    override fun resume(delta: Long) {

    }

    private fun createPropellerBlurredModel(assetsManager: GameAssetManager) {
        val builder = ModelBuilder()
        builder.begin()
        GeneralUtils.createFlatMesh(
            builder,
            "propeller_blurred",
            1F,
            assetsManager.getAssetByDefinition(TexturesDefinitions.PROPELLER_BLURRED)
        )
        propellerBlurredModel = builder.end()
    }

    override fun dispose() {
        propellerBlurredModel.dispose()
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        val currentMap = gameSessionData.currentMap
        playerMovementHandler.update(player, deltaTime, currentMap, services.soundPlayer, services.dispatcher)
        playerShootingHandler.update(
            player, services.dispatcher
        )
    }


    private fun addPlayer(engine: PooledEngine, am: GameAssetManager): Entity {
        EntityBuilder.initialize(engine)
        val apacheModel = am.getAssetByDefinition(ModelsDefinitions.APACHE)
        val startPos = auxVector3_1.set(0F, PLAYER_HEIGHT, 2F)
        val entityBuilder = EntityBuilder.begin().addModelInstanceComponent(apacheModel, startPos)
        if (GameDebugSettings.DISPLAY_PROPELLER) {
            addPropeller(am, entityBuilder)
        }
        val spark0 = TextureRegion(am.getAssetByDefinitionAndIndex(TexturesDefinitions.SPARK, 0))
        val spark1 = TextureRegion(am.getAssetByDefinitionAndIndex(TexturesDefinitions.SPARK, 1))
        val spark2 = TextureRegion(am.getAssetByDefinitionAndIndex(TexturesDefinitions.SPARK, 2))
        val sparkFrames = listOf(spark0, spark1, spark2)
        val priSnd = am.getAssetByDefinition(SfxDefinitions.MACHINE_GUN)
        val secSnd = am.getAssetByDefinition(SfxDefinitions.MISSILE)
        val priDecal = newDecal(PRI_SPARK_SIZE, PRI_SPARK_SIZE, spark0, true)
        val secDecal = newDecal(SEC_SPARK_SIZE, SEC_SPARK_SIZE, spark0, true)
        val priArmProperties = ArmProperties(sparkFrames, priSnd, PRI_RELOAD_DUR, PRI_BULLET_SPEED)
        val secArmProperties = ArmProperties(sparkFrames, secSnd, SEC_RELOAD_DUR, SEC_BULLET_SPEED)
        val priCalculateRelativePosition = object : ArmComponent.CalculateRelativePosition {
            override fun calculate(parent: Entity): Vector3 {
                val transform = ComponentsMapper.modelInstance.get(parent).modelInstance.transform
                val pos = auxVector3_1.set(1F, 0F, 0F).rot(transform).scl(
                    GameSessionData.SPARK_FORWARD_BIAS
                )
                pos.y -= SPARK_HEIGHT_BIAS
                return pos
            }
        }
        val secCalculateRelativePosition = object : ArmComponent.CalculateRelativePosition {
            override fun calculate(parent: Entity): Vector3 {
                playerShootingHandler.secondaryCreationSide =
                    !playerShootingHandler.secondaryCreationSide
                val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
                val pos =
                    auxVector3_1.set(
                        0.2F,
                        0F,
                        if (playerShootingHandler.secondaryCreationSide) 1F else -1F
                    )
                        .rot(transform).scl(SECONDARY_POSITION_BIAS)
                pos.y -= SPARK_HEIGHT_BIAS
                return pos
            }
        }
        return entityBuilder.addAmbSoundComponent(am.getAssetByDefinition(SfxDefinitions.PROPELLER))
            .addCharacterComponent(INITIAL_HP)
            .addPlayerComponent()
            .addPrimaryArmComponent(priDecal, priArmProperties, priCalculateRelativePosition)
            .addSecondaryArmComponent(secDecal, secArmProperties, secCalculateRelativePosition)
            .addSphereCollisionComponent(apacheModel)
            .finishAndAddToEngine()
    }

    private fun addPropeller(
        am: GameAssetManager,
        entityBuilder: EntityBuilder
    ) {
        val propTextureRegion = TextureRegion(am.getAssetByDefinition(TexturesDefinitions.PROPELLER_BLURRED))
        val propDec = newDecal(PROP_SIZE, PROP_SIZE, propTextureRegion, true)
        propDec.rotateX(90F)
        val decals = listOf(ChildDecal(propDec, Vector3.Zero))
        entityBuilder.addChildDecalComponent(decals, true)
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.WEAPON_BUTTON_PRIMARY_PRESSED to PlayerSystemOnWeaponButtonPrimaryPressed(playerShootingHandler),
        SystemEvents.WEAPON_BUTTON_PRIMARY_RELEASED to PlayerSystemOnWeaponButtonPrimaryReleased(playerShootingHandler),
        SystemEvents.WEAPON_BUTTON_SECONDARY_PRESSED to PlayerSystemOnWeaponButtonSecondaryPressed(playerShootingHandler),
        SystemEvents.WEAPON_BUTTON_SECONDARY_RELEASED to PlayerSystemOnWeaponButtonSecondaryReleased(
            playerShootingHandler
        ),
    )

    companion object {
        private const val INITIAL_HP = 100
        private val auxVector3_1 = Vector3()
        private const val PRI_RELOAD_DUR = 125L
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 2F
        private const val PRI_SPARK_SIZE = 0.3F
        private const val SEC_SPARK_SIZE = 0.6F
        private const val PRI_BULLET_SPEED = 32F
        private const val SEC_BULLET_SPEED = 8F
        private const val SECONDARY_POSITION_BIAS = 0.3F
        private const val PLAYER_HEIGHT = 3.9F
        private const val ROTATION_STEP = 2F
    }

    override fun keyDown(keycode: Int): Boolean {
        var handled = false
        when (keycode) {
            Input.Keys.UP -> {
                playerMovementHandler.thrust(player, 4F)
                handled = true
            }

            Input.Keys.LEFT -> {
                playerMovementHandler.rotate(ROTATION_STEP)
                handled = true
            }

            Input.Keys.RIGHT -> {
                playerMovementHandler.rotate(-ROTATION_STEP)
                handled = true
            }
        }
        return handled
    }

    override fun keyUp(keycode: Int): Boolean {
        var handled = false
        if (keycode == Input.Keys.UP) {
            playerMovementHandler.thrust(player, 0F)
            handled = true
        } else if (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT) {
            playerMovementHandler.rotate(0F)
            handled = true
        }
        return handled
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

}
