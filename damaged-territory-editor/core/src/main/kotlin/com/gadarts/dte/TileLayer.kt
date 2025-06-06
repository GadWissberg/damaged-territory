package com.gadarts.dte

data class TileLayer(val name: String, val disabled: Boolean = false) {
    override fun toString(): String {
        return name
    }
}
