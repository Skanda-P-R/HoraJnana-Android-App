package com.hora.jnana.utils

import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DateUtils {
    /**
     * Safely parses a timestamp string to epoch milliseconds.
     * Handles formats with spaces (replaces with 'T') and handles missing offsets by assuming system default.
     */
    fun parseToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        val formatted = dateStr.replace(" ", "T")
        return try {
            // Try parsing with offset/timezone first
            ZonedDateTime.parse(formatted).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                // Fallback to local date time + system default zone
                LocalDateTime.parse(formatted).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                Log.e("DateUtils", "Failed to parse timestamp: $dateStr", e2)
                0L
            }
        }
    }
}
