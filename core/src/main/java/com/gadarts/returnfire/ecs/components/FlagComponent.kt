package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.shared.data.CharacterColor

class FlagComponent(val color: CharacterColor) : Component {

    var follow: Entity? = null
}
