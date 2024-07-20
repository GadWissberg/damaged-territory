package com.gadarts.returnfire.assets.definitions.external

import com.gadarts.returnfire.assets.AssetsTypes

interface ExternalDefinition<T> {
    val fileName: String
    val type: AssetsTypes
}
