package com.gadarts.returnfire.model

import com.badlogic.gdx.ai.pfa.Connection
import com.badlogic.gdx.ai.pfa.DefaultConnection
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.model.graph.GraphNode

class MapGraph(private val width: Int, private val depth: Int) : IndexedGraph<GraphNode> {
    private val nodes: kotlin.Array<kotlin.Array<GraphNode>> =
        Array(depth) { Array(width) { GraphNode(-1, -1, -1, MapGraphType.AVAILABLE) } }
    private val connections = mutableMapOf<GraphNode, Array<Connection<GraphNode>>>()

    fun addNode(x: Int, y: Int, type: MapGraphType): GraphNode {
        val index = y * width + x
        val node = GraphNode(index, x, y, type)
        nodes[y][x] = node
        return node
    }

    fun connect(from: GraphNode, to: GraphNode) {
        connections.computeIfAbsent(from) { Array() }.add(DefaultConnection(from, to))
    }

    override fun getIndex(node: GraphNode): Int = node.index

    override fun getConnections(fromNode: GraphNode): Array<Connection<GraphNode>> =
        connections[fromNode] ?: Array()

    override fun getNodeCount(): Int = width * depth

    fun getNode(x: Int, y: Int): GraphNode {
        return nodes[y][x]
    }
}
