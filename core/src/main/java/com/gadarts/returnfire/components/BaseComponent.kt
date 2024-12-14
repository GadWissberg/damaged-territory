package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.components.character.CharacterColor

class BaseComponent(val color: CharacterColor) : Component {
    fun init(westDoor: Entity, eastDoor: Entity) {
        this.westDoor = westDoor
        this.eastDoor = eastDoor
    }

    var doorMoveState: Int = 1
        private set

    fun close() {
        doorMoveState = -1
    }

    fun isIdle(): Boolean {
        return doorMoveState == 0
    }

    fun setIdle() {
        doorMoveState = 0
    }

    var baseDoorSoundId: Long = -1L
    lateinit var eastDoor: Entity
    lateinit var westDoor: Entity

}
