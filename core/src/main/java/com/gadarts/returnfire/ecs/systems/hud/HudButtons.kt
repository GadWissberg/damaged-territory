package com.gadarts.returnfire.ecs.systems.hud

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.events.SystemEvents

class HudButtons(private val gamePlayManagers: GamePlayManagers) {
    var onboardButton: ImageButton? = null
    var manualAimButton: ImageButton? = null

    val priWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    val secWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    val reverseButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }
    val onBoardButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_ONBOARD_PRESSED.ordinal)
            if (onboardButton != null) {
                onboardButton!!.isVisible = false
            }
            return super.touchDown(event, x, y, pointer, button)
        }
    }
    val manualAimButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_MANUAL_AIM_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }
    }
}
