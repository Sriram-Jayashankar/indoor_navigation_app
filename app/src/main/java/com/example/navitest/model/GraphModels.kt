package com.example.navitest.model

data class Node(val id: Int, val x: Float, val y: Float)
data class Edge(val fromId: Int, val toId: Int)
data class Router(
    val id: Int,
    val x: Float,
    val y: Float,
    val ssid: String
)