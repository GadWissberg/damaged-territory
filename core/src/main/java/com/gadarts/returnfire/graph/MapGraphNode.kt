package com.gadarts.returnfire.graph

data class MapGraphNode(val index: Int, val x: Int, val y: Int, var type: MapGraphType) {
    override fun toString(): String = "Node: $index, x: $x, y: $y, type: $type"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val node = other as MapGraphNode
        return x == node.x && y == node.y
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + type.hashCode()
        return result
    }
}
