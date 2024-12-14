package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class BaseComponent : Component {
    private var baseDoorSoundId: Long = -1L
    private lateinit var eastDoor: Entity
    private lateinit var westDoor: Entity

}
