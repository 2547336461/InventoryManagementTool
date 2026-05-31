package com.inventory.manager.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatDate(timestamp: Long?): String =
        if (timestamp == null) "-" else dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun parseDate(dateStr: String): Long? = try {
        dateFormat.parse(dateStr)?.time
    } catch (e: Exception) { null }

    fun today(): Long = System.currentTimeMillis()

    fun daysFromNow(days: Int): Long = System.currentTimeMillis() + days.toLong() * 24 * 60 * 60 * 1000

    fun daysUntil(timestamp: Long): Int =
        ((timestamp - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
}
