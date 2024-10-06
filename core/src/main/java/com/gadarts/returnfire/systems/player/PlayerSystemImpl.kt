package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.MovementTouchPadListener
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.TurretTouchPadListener
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnPhysicsSystemReady
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryReleased
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryReleased

class PlayerSystemImpl : GameEntitySystem(), PlayerSystem, InputProcessor {

    private val playerShootingHandler = PlayerShootingHandler()
    private val playerFactory by lazy {
        PlayerFactory(
            managers.assetsManager,
            gameSessionData,
            playerShootingHandler
        )
    }
    private lateinit var playerMovementHandler: VehicleMovementHandler

    override fun onSystemReady() {
        super.onSystemReady()
        playerMovementHandler = createPlayerMovementHandler()
        playerMovementHandler.initialize(gameSessionData.renderData.camera)
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
                TankMovementHandlerDesktop(rigidBody)
            }
        }
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
                playerMovementHandler.onMovementTouchpadTouchUp(keycode)
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
            map.placedElements.find { placedElement -> placedElement.definition == GameDebugSettings.SELECTED_VEHICLE }
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
            gameSessionData.player
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
