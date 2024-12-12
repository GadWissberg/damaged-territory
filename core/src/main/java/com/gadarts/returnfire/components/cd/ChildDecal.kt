package com.gadarts.returnfire.components.cd

import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

class ChildDecal(
    val decal: Decal,
    val relativePosition: Vector3,
    val localRotation: Quaternion?,
    val decalAnimation: DecalAnimation? = null
) {
    var visible: Boolean = true
    var rotationStep = Vector2(1F, 0F)

}
