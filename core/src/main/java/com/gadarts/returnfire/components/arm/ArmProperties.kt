package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureRegion

class ArmProperties(
    val sparkFrames: List<TextureRegion>,
    val shootingSound: Sound,
    val reloadDuration: Long,
    val speed: Float
)
