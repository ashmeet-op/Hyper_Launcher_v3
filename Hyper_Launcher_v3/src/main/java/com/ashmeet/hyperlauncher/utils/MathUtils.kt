package com.ashmeet.hyperlauncher.utils

import kotlin.math.hypot

object MathUtils {

    @JvmStatic
    fun map(x: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return ((x - inMin) * (outMax - outMin) / (inMax - inMin)) + outMin
    }

    @JvmStatic
    fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x2 - x1).toDouble()
        val y = (y2 - y1).toDouble()
        return hypot(x, y).toFloat()
    }

    @JvmStatic
    fun dist(dx: Float, dy: Float): Float {
        return hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    @JvmStatic
    fun <T> findNearestPositive(targetValue: Int, objects: List<T>, valueProvider: ValueProvider<T>): RankedValue<T>? {
        var delta = Int.MAX_VALUE
        var selectedObject: T? = null
        for (obj in objects) {
            val objectValue = valueProvider.getValue(obj)
            if (objectValue < targetValue) continue

            val currentDelta = objectValue - targetValue
            if (currentDelta == 0) return RankedValue(obj, 0)
            if (currentDelta >= delta) continue

            selectedObject = obj
            delta = currentDelta
        }
        return selectedObject?.let { RankedValue(it, delta) }
    }

    fun interface ValueProvider<T> {
        fun getValue(obj: T): Int
    }

    class RankedValue<T>(
        @JvmField val value: T,
        @JvmField val rank: Int,
    )

    @JvmStatic
    fun <T> objectMin(object1: T?, object2: T?, valueProvider: ValueProvider<T>): T? {
        if (object1 == null) return object2
        if (object2 == null) return object1
        val value1 = valueProvider.getValue(object1)
        val value2 = valueProvider.getValue(object2)
        return if (value1 <= value2) object1 else object2
    }
}
