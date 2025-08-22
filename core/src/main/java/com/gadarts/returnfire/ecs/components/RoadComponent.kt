package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.MathUtils
import com.gadarts.shared.assets.definitions.external.TextureDefinition

class RoadComponent(val textureDefinition: TextureDefinition) : Component {
    var hp: Int = MathUtils.random(1, 3)
        private set

    fun takeDamage() {
        hp -= 1
    }

}
