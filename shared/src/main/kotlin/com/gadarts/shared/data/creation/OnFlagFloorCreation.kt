package com.gadarts.shared.data.creation

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.AmbDefinition

class OnFlagFloorCreation(private val color: CharacterColor) : OnAmbCreation {
    override fun invoke(
        mapInflater: MapInflater,
        position: Vector3,
        def: AmbDefinition,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    ) {
        mapInflater.addAmbObject(
            position,
            if (color == CharacterColor.BROWN) AmbDefinition.FLAG_BROWN else AmbDefinition.FLAG_GREEN,
            exculdedTiles,
            null
        )
    }

}
