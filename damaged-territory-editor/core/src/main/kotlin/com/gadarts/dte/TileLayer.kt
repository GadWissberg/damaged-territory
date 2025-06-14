package com.gadarts.dte

data class TileLayer(
    val name: String,
    val disabled: Boolean = false,
    val tiles: Array<Array<PlacedTile?>>,
    val bitMap: Array<Array<Int>>
) {
    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TileLayer

        if (disabled != other.disabled) return false
        if (name != other.name) return false
        if (!tiles.contentDeepEquals(other.tiles)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = disabled.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + tiles.contentDeepHashCode()
        return result
    }
}
