package com.gadarts.shared.assets.definitions.external

import com.gadarts.shared.assets.AssetsTypes

interface ExternalDefinition<T> {
    val fileName: String
    val type: AssetsTypes
}
