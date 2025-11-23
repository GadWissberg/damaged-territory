package com.gadarts.dte.scene

import com.gadarts.shared.data.definitions.ElementDefinition

class PlaceableObject(val definition: ElementDefinition, val color: com.gadarts.shared.data.CharacterColor?) {
    override fun toString(): String {
        return "${definition.getName()}${if (color != null) "_" + color.name else ""}"
    }
}
