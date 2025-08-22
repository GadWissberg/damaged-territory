package com.gadarts.returnfire.ecs.systems.hud

import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

interface HudSystem {
    fun addButton(
        ui: Table,
        iconDefinition: String,
        clickListener: ClickListener,
        rightPadding: Float = 0F,
        visible: Boolean = true,
    ): Cell<ImageButton>

    fun addTouchpad(ui: Table, touchpad: Touchpad): Cell<Touchpad>
    fun addRadar(table: Table): Cell<com.gadarts.returnfire.ecs.systems.hud.radar.Radar>

    companion object {
        const val JOYSTICK_PADDING = 64F
    }
}
