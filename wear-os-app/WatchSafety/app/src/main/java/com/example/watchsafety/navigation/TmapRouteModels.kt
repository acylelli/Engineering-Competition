package com.example.watchsafety.navigation

data class TmapRouteResult(

    val totalDistanceMeters: Int,

    val totalTimeSeconds: Int,

    val steps: List<NavigationStep>
)

data class NavigationStep(

    val index: Int,

    val turnType: Int,

    val description: String,

    val longitude: Double,

    val latitude: Double
)