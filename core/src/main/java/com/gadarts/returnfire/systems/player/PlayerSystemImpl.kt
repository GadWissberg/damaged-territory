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
import com.gadarts.returnfire.systems.player.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.systems.player.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.movement.tank.TankMovementHandlerMobile
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnPhysicsSystemReady
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonPrimaryReleased
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryPressed
import com.gadarts.returnfire.systems.player.react.PlayerSystemOnWeaponButtonSecondaryReleased

class PlayerSystemImpl : GameEntitySystem(), PlayerSystem, InputProcessor {

    private val playerShootingHandler = PlayerShootingHandler()
    private val playerFactory by lazy { PlayerFactory(managers.assetsManager, gameSessionData, playerShootingHandler) }

    private val playerMovementHandler: VehicleMovementHandler by lazy {
        createPlayerMovementHandler()
    }

    private fun createPlayerMovementHandler(): VehicleMovementHandler {
        val characterDefinition = ComponentsMapper.character.get(gameSessionData.player).definition
        val runsOnMobile = gameSessionData.runsOnMobile
        return if (characterDefinition == SimpleCharacterDefinition.APACHE) {
            if (runsOnMobile) {
                ApacheMovementHandlerMobile()
            } else {
                ApacheMovementHandlerDesktop()
            }
        } else {
            if (runsOnMobile) {
                TankMovementHandlerMobile()
            } else {
                TankMovementHandlerDesktop()
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
        val map = managers.assetsManager.getAssetByDefinition(MapDefinition.MAP_0)
        val placedPlayer =
            map.placedElements.find { placedElement -> placedElement.definition == GameDebugSettings.SELECTED_VEHICLE }
        val player = playerFactory.create(placedPlayer!!)
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


}
