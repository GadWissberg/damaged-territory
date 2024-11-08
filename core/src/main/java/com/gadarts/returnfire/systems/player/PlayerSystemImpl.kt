package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.screens.GamePlayScreen
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.SystemEvents.*
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.MovementTouchPadListener
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.TurretTouchPadListener
import com.gadarts.returnfire.systems.player.react.*

/**
 * Responsible to initialize input, create the player and updates the player's character according to the input.
 */
class PlayerSystemImpl : GameEntitySystem(), PlayerSystem, InputProcessor {

    private val playerShootingHandler = PlayerShootingHandler()
    private val playerFactory by lazy {
        PlayerFactory(
            managers.assetsManager,
            gameSessionData,
            playerShootingHandler,
            managers.factories.gameModelInstanceFactory
        )
    }
    private val playerMovementHandler: VehicleMovementHandler by lazy {
        val playerMovementHandler = createPlayerMovementHandler()
        playerMovementHandler.initialize(gameSessionData.renderData.camera)
        playerMovementHandler
    }

    override fun onSystemReady() {
        super.onSystemReady()
        initInputMethod()
    }

    private fun createPlayerMovementHandler(): VehicleMovementHandler {
        val player = gameSessionData.player
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
        BUTTON_WEAPON_PRIMARY_PRESSED to PlayerSystemOnWeaponButtonPrimaryPressed(playerShootingHandler),
        BUTTON_WEAPON_PRIMARY_RELEASED to PlayerSystemOnWeaponButtonPrimaryReleased(playerShootingHandler),
        BUTTON_WEAPON_SECONDARY_PRESSED to PlayerSystemOnWeaponButtonSecondaryPressed(playerShootingHandler),
        BUTTON_WEAPON_SECONDARY_RELEASED to PlayerSystemOnWeaponButtonSecondaryReleased(playerShootingHandler),
        BUTTON_REVERSE_PRESSED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                playerMovementHandler.onReverseScreenButtonPressed()
            }
        },
        BUTTON_REVERSE_RELEASED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                playerMovementHandler.onReverseScreenButtonReleased()
            }
        },
        PHYSICS_SYSTEM_READY to PlayerSystemOnPhysicsSystemReady(),
        CHARACTER_DIED to PlayerSystemOnCharacterDied(),
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
            deltaTime
        )
        playerShootingHandler.update()
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.UP -> {
                playerMovementHandler.thrust(gameSessionData.player)
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
                playerShootingHandler.startSecondaryShooting()
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

    override fun keyUp(keycode: Int): Boolean {
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
        val placedPlayer =
            map.placedElements.find { placedElement -> placedElement.definition == GamePlayScreen.SELECTED_VEHICLE }
        val player = playerFactory.create(placedPlayer!!)
        engine.addEntity(player)
        gameSessionData.player = player
        ComponentsMapper.modelInstance.get(player).hidden = GameDebugSettings.HIDE_PLAYER
        initializePlayerHandlers()
        return player
    }

    private fun initializePlayerHandlers() {
        playerShootingHandler.initialize(
            managers.dispatcher,
            gameSessionData
        )
    }

    private fun initInputMethod() {
        if (gameSessionData.runsOnMobile) {
            gameSessionData.gameSessionDataHud.movementTouchpad.addListener(
                MovementTouchPadListener(
                    playerMovementHandler,
                    gameSessionData.player
                )
            )
            gameSessionData.gameSessionDataHud.turretTouchpad.addListener(
                TurretTouchPadListener(
                    playerMovementHandler,
                    playerShootingHandler
                )
            )
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
        }
    }


}
