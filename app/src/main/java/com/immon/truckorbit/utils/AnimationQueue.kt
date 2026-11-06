package com.immon.truckorbit.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

class AnimationQueue(
    @Volatile private var startPosition: Any,
    private val scope: LifecycleCoroutineScope,
    private val updatePosition: (Double, Double) -> Unit
) {

    private val items = Collections.synchronizedList(mutableListOf<Pair<Double, Double>>())
    private var isRunning = false

    init {
        val (lat, lng) = extractLatLng(startPosition)
        items.add(Pair(lat, lng))
        startAnimation()
    }

    fun addToQueue(newPosition: Any, threshold: Float = 0f) {
        val (newLat, newLng) = extractLatLng(newPosition)
        val (lastLat, lastLng) = items.lastOrNull() ?: Pair(0.0, 0.0)

        if (threshold > 0f) {
            val distance = Math.sqrt(Math.pow(newLat - lastLat, 2.0) + Math.pow(newLng - lastLng, 2.0))
            if (distance < threshold) return
        }

        items.add(Pair(newLat, newLng))
        if (!isRunning) startAnimation()
    }

    private fun startAnimation() {
        if (isRunning || items.isEmpty()) return
        isRunning = true

        scope.launch(Dispatchers.Default) {
            while (items.size > 1) {
                val start = items[0]
                val end = items[1]

                val steps = 10
                for (i in 1..steps) {
                    val fraction = i.toDouble() / steps
                    val lat = start.first + (end.first - start.first) * fraction
                    val lng = start.second + (end.second - start.second) * fraction

                    launch(Dispatchers.Main) {
                        updatePosition(lat, lng)
                    }
                    delay(50)
                }
                items.removeAt(0)
            }
            isRunning = false
        }
    }

    private fun extractLatLng(position: Any): Pair<Double, Double> {
        return when (position) {
            is org.osmdroid.util.GeoPoint -> Pair(position.latitude, position.longitude)
            else -> throw IllegalArgumentException("Unsupported position type")
        }
    }
}
