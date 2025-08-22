package com.gadarts.returnfire.ecs.components.amb

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.audio.Sound

class AmbSoundComponent(val sound: Sound) : Component {
    var soundId: Long = -1L
    var pitch = 1F
    var pitchTarget = 1F
}
