package com.gadarts.returnfire.model.graph

data class GraphNode(val index: Int, val x: Int, val y: Int) {
    override fun toString(): String = "Node($index, x=$x, y=$y)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val node = other as GraphNode
        return x == node.x && y == node.y
    }
}
