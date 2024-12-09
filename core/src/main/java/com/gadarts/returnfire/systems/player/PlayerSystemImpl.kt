package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btConeShape
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.StageComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.SystemEvents.*
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_ENEMY
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_PLAYER
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.MovementTouchPadListener
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.TurretTouchPadListener
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnCharacterDied
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnCharacterOnboarded

/**
 * Responsible to initialize input, create the player and updates the player's character according to the input.
 */
class PlayerSystemImpl(managers: Managers) : GameEntitySystem(managers), PlayerSystem, InputProcessor {

    private val stage: Entity by lazy {
        engine.getEntitiesFor(
            Family.all(StageComponent::class.java).get()
        ).first()
    }
    private val autoAim by lazy {
        val ghostObject = btPairCachingGhostObject()
        ghostObject.collisionShape = btConeShape(0.5F, PlayerSystem.AUTO_AIM_HEIGHT)
        ghostObject.collisionFlags = btCollisionObject.CollisionFlags.CF_NO_CONTACT_RESPONSE
        gameSessionData.physicsData.collisionWorld.addCollisionObject(
            ghostObject,
            COLLISION_GROUP_PLAYER,
            COLLISION_GROUP_ENEMY
        )
        ghostObject
    }
    private val playerShootingHandler = PlayerShootingHandler(managers.entityBuilder)
    private val playerFactory by lazy {
        PlayerFactory(
            managers.assetsManager,
            gameSessionData,
            playerShootingHandler,
            managers.factories.gameModelInstanceFactory,
            managers.entityBuilder
        )
    }
    private val playerMovementHandler: VehicleMovementHandler by lazy {
        val playerMovementHandler = createPlayerMovementHandler()
        playerMovementHandler.initialize(gameSessionData.renderData.camera)
        playerMovementHandler
    }

    private fun createPlayerMovementHandler(): VehicleMovementHandler {
        val player = gameSessionData.gameplayData.player
        val characterDefinition = ComponentsMapper.character.get(player).definition
        val runsOnMobile = gameSessionData.runsOnMobile
        return if (characterDefinition == SimpleCharacterDefinition.APACHE) {
            if (runsOnMobile) {
                ApacheMovementHandlerMobile()
            } else {
                ApacheMovementHandlerDesktop()
            }
        } else {
            val rigidBody = ComponentsMapper.physics.get(player).rigidBody
            if (runsOnMobile) {
                TankMovementHandlerMobile(rigidBody, player)
            } else {
                TankMovementHandlerDesktop(rigidBody, player)
            }
        }
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        BUTTON_WEAPON_PRIMARY_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                playerShootingHandler.startPrimaryShooting()
            }
        },
        BUTTON_WEAPON_PRIMARY_RELEASED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                playerShootingHandler.stopPrimaryShooting()
            }
        },
        BUTTON_WEAPON_SECONDARY_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                playerShootingHandler.startSecondaryShooting()
            }
        },
        BUTTON_WEAPON_SECONDARY_RELEASED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                playerShootingHandler.stopSecondaryShooting()
            }
        },
        BUTTON_REVERSE_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                playerMovementHandler.onReverseScreenButtonPressed()
            }
        },
        BUTTON_REVERSE_RELEASED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                playerMovementHandler.onReverseScreenButtonReleased()
            }
        },
        CHARACTER_ONBOARDED to PlayerSystemOnCharacterOnboarded(this),
        CHARACTER_DIED to PlayerSystemOnCharacterDied(),
        BUTTON_ONBOARD_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                onboard()
            }
        }
    )

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        addPlayer()
    }

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        autoAim.dispose()
    }

    override fun update(deltaTime: Float) {
        val player = gameSessionData.gameplayData.player
        if (ComponentsMapper.boarding.get(player).isBoarding() || ComponentsMapper.character.get(player).dead) return

        playerMovementHandler.update(
            player,
            deltaTime
        )
        playerShootingHandler.update()
        val position =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val stagePosition =
            ComponentsMapper.modelInstance.get(stage).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector2
            )
        val childDecalComponent = ComponentsMapper.childDecal.get(stage)
        handleLandingIndicatorVisibility(childDecalComponent, position, stagePosition)
    }

    private fun handleLandingIndicatorVisibility(
        childDecalComponent: ChildDecalComponent,
        position: Vector3,
        stagePosition: Vector3
    ) {
        val oldValue = childDecalComponent.visible
        val newValue = (position.x <= stagePosition.x + LANDING_OK_OFFSET
            && position.x >= stagePosition.x - LANDING_OK_OFFSET
            && position.z <= stagePosition.z + LANDING_OK_OFFSET
            && position.z >= stagePosition.z - LANDING_OK_OFFSET)
        childDecalComponent.visible = newValue
        if (oldValue != newValue) {
            managers.dispatcher.dispatchMessage(
                LANDING_INDICATOR_VISIBILITY_CHANGED.ordinal,
                newValue
            )
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        val onboardingComponent = ComponentsMapper.boarding.get(gameSessionData.gameplayData.player)
        if (onboardingComponent.isBoarding()) return false

        when (keycode) {
            Input.Keys.UP -> {
                playerMovementHandler.thrust(gameSessionData.gameplayData.player)
            }

            Input.Keys.DOWN -> {
                playerMovementHandler.reverse()
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
                if (ComponentsMapper.childDecal.get(stage).visible) {
                    onboard()
                } else {
                    playerShootingHandler.startSecondaryShooting()
                }
            }

            Input.Keys.A -> {
                playerMovementHandler.letterPressedA()
            }

            Input.Keys.D -> {
                playerMovementHandler.letterPressedD()
            }
        }
        return false
    }

    private fun onboard() {
        ComponentsMapper.boarding.get(gameSessionData.gameplayData.player).onBoard()
        managers.dispatcher.dispatchMessage(CHARACTER_BOARDING.ordinal)
    }

    override fun keyUp(keycode: Int): Boolean {
        if (ComponentsMapper.boarding.get(gameSessionData.gameplayData.player).isBoarding()) return false

        when (keycode) {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                playerMovementHandler.onMovementTouchPadTouchUp(keycode)
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.stopPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                playerShootingHandler.stopSecondaryShooting()
            }

            Input.Keys.A -> {
                playerMovementHandler.letterReleasedA()
            }

            Input.Keys.D -> {
                playerMovementHandler.letterReleasedD()
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
        val map = managers.assetsManager.getAssetByDefinition(MapDefinition.MAP_0)
        val base =
            map.placedElements.find { placedElement -> placedElement.definition == AmbDefinition.BASE }
        val player = playerFactory.create(base!!, gameSessionData.selected)
        engine.addEntity(player)
        gameSessionData.gameplayData.player = player
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(player)
        modelInstanceComponent.hidden = GameDebugSettings.HIDE_PLAYER
        initializePlayerHandlers()
        return player
    }

    private fun initializePlayerHandlers() {
        playerShootingHandler.initialize(
            managers.dispatcher,
            gameSessionData,
            autoAim
        )
    }

    override fun initInputMethod() {
        if (gameSessionData.runsOnMobile) {
            gameSessionData.hudData.movementTouchpad.addListener(
                MovementTouchPadListener(
                    playerMovementHandler,
                    gameSessionData.gameplayData.player
                )
            )
            gameSessionData.hudData.turretTouchpad.addListener(
                TurretTouchPadListener(
                    playerMovementHandler,
                    playerShootingHandler
                )
            )
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val LANDING_OK_OFFSET = 0.45F
    }
}
