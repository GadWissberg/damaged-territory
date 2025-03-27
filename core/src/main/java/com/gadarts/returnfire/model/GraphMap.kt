package com.gadarts.returnfire.model

import com.badlogic.gdx.ai.pfa.Connection
import com.badlogic.gdx.ai.pfa.DefaultConnection
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.model.graph.GraphNode

class GraphMap(private val width: Int) : IndexedGraph<GraphNode> {
    private val nodes = mutableListOf<GraphNode>()
    private val connections = mutableMapOf<GraphNode, Array<Connection<GraphNode>>>()

    fun addNode(x: Int, y: Int): GraphNode {
        val node = GraphNode(nodes.size, x, y)
        nodes.add(node)
        return node
    }

    fun connect(from: GraphNode, to: GraphNode) {
        connections.computeIfAbsent(from) { Array() }.add(DefaultConnection(from, to))
    }

    override fun getIndex(node: GraphNode): Int = node.index

    override fun getConnections(fromNode: GraphNode): Array<Connection<GraphNode>> =
        connections[fromNode] ?: Array()

    override fun getNodeCount(): Int = nodes.size
    fun getNode(x: Int, y: Int): GraphNode {
        return nodes[y * width + x]
    }
}