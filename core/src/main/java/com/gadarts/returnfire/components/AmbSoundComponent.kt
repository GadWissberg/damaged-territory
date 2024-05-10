package com.gadarts.returnfire.components

import com.badlogic.gdx.audio.Sound

class AmbSoundComponent : GameComponent() {
    var soundId = -1L
    lateinit var sound: Sound

    fun init(sound: Sound) {
        this.sound = sound
        this.soundId = -1L
    }

    override fun reset() {
    }

}
