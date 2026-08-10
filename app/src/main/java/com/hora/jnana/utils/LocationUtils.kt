package com.hora.jnana.utils

import java.util.Locale

import kotlin.math.abs

object LocationUtils {
    fun formatCoord(value: Double?): String? {
        if (value == null) return null
        return "%.4f".format(Locale.US, value)
    }

    fun isSignificantChange(oldLat: Double?, oldLon: Double?, newLat: Double?, newLon: Double?): Boolean {
        // If both are null, it's not a change
        if (oldLat == null && newLat == null && oldLon == null && newLon == null) return false
        
        // If one is null and the other isn't, it is a change
        if (oldLat == null || oldLon == null || newLat == null || newLon == null) return true
        
        // Otherwise check the delta
        return abs(oldLat - newLat) >= 1.0 || abs(oldLon - newLon) >= 1.0
    }
}
