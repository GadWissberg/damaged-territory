package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal.newDecal
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.assets.ModelDefinition
import com.gadarts.returnfire.assets.SfxDefinitions
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GameModelInstance
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.GameSessionData.Companion.SPARK_HEIGHT_BIAS
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryReleased
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryReleased

class PlayerSystem : GameEntitySystem() {

    private val playerShootingHandler = PlayerShootingHandler()
    private val playerMovementHandler = PlayerMovementHandler()
    private var lastTouchDown: Long = 0

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        addPlayer()
        gameSessionData.touchpad.addListener(object : ClickListener() {
            override fun touchDown(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                playerMovementHandler.onTouchDown(lastTouchDown, gameSessionData.player)
                touchPadTouched(event!!.target)
                lastTouchDown = TimeUtils.millis()
                return super.touchDown(event, x, y, pointer, button)
            }

            override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int) {
                touchPadTouched(event!!.target)
                super.touchDragged(event, x, y, pointer)
            }

            override fun touchUp(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ) {
                playerMovementHandler.onTouchUp()
                super.touchUp(event, x, y, pointer, button)
            }
        })
        playerShootingHandler.initialize(
            services.dispatcher,
            services.engine,
            gameSessionData.priBulletsPool,
            gameSessionData.secBulletsPool,
            gameSessionData.player
        )
        playerMovementHandler.initialize(
            services.assetsManager,
            gameSessionData.camera,
        )
    }

    override fun resume(delta: Long) {

    }

    private fun touchPadTouched(actor: Actor) {
        val deltaX = (actor as Touchpad).knobPercentX
        val deltaY = actor.knobPercentY
        playerMovementHandler.onTouchPadTouched(deltaX, deltaY, gameSessionData.player)
    }

    override fun dispose() {

    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        val currentMap = gameSessionData.currentMap
        playerMovementHandler.update(
            gameSessionData.player,
            deltaTime,
            currentMap,
            services.dispatcher
        )
        playerShootingHandler.update()
    }

    private fun addPlayer(): Entity {
        EntityBuilder.initialize(services.engine)
        val apacheModel = services.assetsManager.getAssetByDefinition(ModelDefinition.APACHE)
        val entityBuilder =
            EntityBuilder.begin()
                .addModelInstanceComponent(
                    GameModelInstance(
                        ModelInstance(apacheModel),
                        services.assetsManager.getCachedBoundingBox(ModelDefinition.APACHE)
                    ),
                    auxVector3_1.set(0F, PLAYER_HEIGHT, 2F),
                    false
                )
        if (GameDebugSettings.DISPLAY_PROPELLER) {
            addPropeller(entityBuilder)
        }
        val spark0 = TextureRegion(
            services.assetsManager.getAssetByDefinitionAndIndex(
                TexturesDefinitions.SPARK,
                0
            )
        )
        val spark1 = TextureRegion(
            services.assetsManager.getAssetByDefinitionAndIndex(
                TexturesDefinitions.SPARK,
                1
            )
        )
        val spark2 = TextureRegion(
            services.assetsManager.getAssetByDefinitionAndIndex(
                TexturesDefinitions.SPARK,
                2
            )
        )
        val sparkFrames = listOf(spark0, spark1, spark2)
        entityBuilder.addAmbSoundComponent(
            services.assetsManager.getAssetByDefinition(
                SfxDefinitions.PROPELLER
            )
        )
            .addCharacterComponent(INITIAL_HP)
            .addPlayerComponent()
        addPrimaryArmComponent(entityBuilder, sparkFrames)
        val player = addSecondaryArmComponent(entityBuilder, sparkFrames)
            .addSphereCollisionComponent(apacheModel)
            .finishAndAddToEngine()
        gameSessionData.player = player
        ComponentsMapper.modelInstance.get(player).hidden = GameDebugSettings.HIDE_PLAYER
        return player
    }

    private fun addPrimaryArmComponent(
        entityBuilder: EntityBuilder,
        sparkFrames: List<TextureRegion>
    ): EntityBuilder {
        val priSnd = services.assetsManager.getAssetByDefinition(SfxDefinitions.MACHINE_GUN)
        val priDecal = newDecal(PRI_SPARK_SIZE, PRI_SPARK_SIZE, sparkFrames.first(), true)
        val priArmProperties = ArmProperties(sparkFrames, priSnd, PRI_RELOAD_DUR, PRI_BULLET_SPEED)
        val priCalculateRelativePosition = object : ArmComponent.CalculateRelativePosition {
            override fun calculate(parent: Entity): Vector3 {
                val transform =
                    ComponentsMapper.modelInstance.get(parent).gameModelInstance.modelInstance.transform
                val pos = auxVector3_1.set(1F, 0F, 0F).rot(transform).scl(
                    GameSessionData.SPARK_FORWARD_BIAS
                )
                pos.y -= SPARK_HEIGHT_BIAS
                return pos
            }
        }
        entityBuilder.addPrimaryArmComponent(
            priDecal,
            priArmProperties,
            priCalculateRelativePosition
        )
        return entityBuilder
    }

    private fun addSecondaryArmComponent(
        entityBuilder: EntityBuilder,
        sparkFrames: List<TextureRegion>
    ): EntityBuilder {
        val secSnd = services.assetsManager.getAssetByDefinition(SfxDefinitions.MISSILE)
        val secArmProperties = ArmProperties(sparkFrames, secSnd, SEC_RELOAD_DUR, SEC_BULLET_SPEED)
        val secDecal = newDecal(SEC_SPARK_SIZE, SEC_SPARK_SIZE, sparkFrames.first(), true)
        val secCalculateRelativePosition = object : ArmComponent.CalculateRelativePosition {
            override fun calculate(parent: Entity): Vector3 {
                playerShootingHandler.secondaryCreationSide =
                    !playerShootingHandler.secondaryCreationSide
                val transform =
                    ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform
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
        entityBuilder.addSecondaryArmComponent(
            secDecal,
            secArmProperties,
            secCalculateRelativePosition
        )
        return entityBuilder
    }

    private fun addPropeller(
        entityBuilder: EntityBuilder
    ) {
        val propTextureRegion =
            TextureRegion(services.assetsManager.getAssetByDefinition(TexturesDefinitions.PROPELLER_BLURRED))
        val propDec = newDecal(PROP_SIZE, PROP_SIZE, propTextureRegion, true)
        propDec.rotateX(90F)
        val decals = listOf(ChildDecal(propDec, Vector3.Zero))
        entityBuilder.addChildDecalComponent(decals, true)
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
    )

    companion object {
        private const val INITIAL_HP = 100
        private val auxVector3_1 = Vector3()
        private const val PRI_RELOAD_DUR = 125L
        private const val SEC_RELOAD_DUR = 2000L
        private const val PROP_SIZE = 1.5F
        private const val PRI_SPARK_SIZE = 0.3F
        private const val SEC_SPARK_SIZE = 0.6F
        private const val PRI_BULLET_SPEED = 16F
        private const val SEC_BULLET_SPEED = 5F
        private const val SECONDARY_POSITION_BIAS = 0.3F
        private const val PLAYER_HEIGHT = 3.9F
    }

}
