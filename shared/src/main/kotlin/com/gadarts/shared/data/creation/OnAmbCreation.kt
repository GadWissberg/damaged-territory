package com.gadarts.shared.data.creation

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.data.definitions.AmbDefinition

interface OnAmbCreation {
    fun invoke(
        mapInflater: MapInflater,
        position: Vector3,
        def: AmbDefinition,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    )

}
