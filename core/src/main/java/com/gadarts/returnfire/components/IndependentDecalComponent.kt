package com.gadarts.returnfire.components

import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.utils.TimeUtils

class IndependentDecalComponent : GameComponent() {
    var ttl: Long = 0L
    lateinit var decal: Decal

    override fun reset() {

    }

    fun init(decal: Decal, lifeInMillis: Long) {
        this.decal = decal
        this.ttl = TimeUtils.millis() + lifeInMillis
    }

}
