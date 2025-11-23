package com.gadarts.shared.data.creation

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.AmbDefinition

interface MapInflater {
    fun addAmbObject(
        position: Vector3,
        ambDefinition: AmbDefinition,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
        rotation: Float?,
        color: CharacterColor?
    )

}
