package com.gadarts.dte.scene

import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.model.definitions.ElementDefinition

class SelectionData(
    var selectedObject: ElementDefinition? = null,
    var selectedMode: Modes = Modes.TILES,
    var selectedTile: TextureDefinition? = null,
    var selectedLayerIndex: Int = 1,
) {
}
