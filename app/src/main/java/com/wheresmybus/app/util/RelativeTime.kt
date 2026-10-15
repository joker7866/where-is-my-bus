package com.wheresmybus.app.util

import java.util.concurrent.TimeUnit

fun formatRelativeTimeAgo(epochMillis: Long): String {
    val diffMs = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${TimeUnit.MINUTES.toHours(minutes)} hr ago"
        else -> {
            val days = TimeUnit.MINUTES.toDays(minutes)
            "$days day${if (days == 1L) "" else "s"} ago"
        }
    }
}
