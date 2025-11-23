package com.gadarts.dte.scene

import com.gadarts.shared.assets.definitions.external.TextureDefinition

class SelectionData(
    var placeableObject: PlaceableObject? = null,
    var selectedMode: Modes = Modes.TILES,
    var selectedTile: TextureDefinition? = null,
    var selectedLayerIndex: Int = 1,
)
