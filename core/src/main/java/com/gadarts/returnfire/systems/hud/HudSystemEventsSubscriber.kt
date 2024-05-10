package com.gadarts.returnfire.systems.hud

import com.gadarts.returnfire.systems.SystemEventsSubscriber

interface HudSystemEventsSubscriber : SystemEventsSubscriber {
    fun onPrimaryWeaponButtonPressed()
    fun onPrimaryWeaponButtonReleased()
    fun onSecondaryWeaponButtonPressed()
    fun onSecondaryWeaponButtonReleased()

}
