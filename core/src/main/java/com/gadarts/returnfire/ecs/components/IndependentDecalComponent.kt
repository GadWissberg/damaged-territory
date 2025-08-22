package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.utils.TimeUtils

class IndependentDecalComponent(val decal: Decal, lifeInMillis: Long) : Component {
    var ttl: Long = 0L

    init {
        this.ttl = TimeUtils.millis() + lifeInMillis
    }

}
