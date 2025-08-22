package tech.arcent.achievements.data.remote.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
 Parsing utilities for remote achievements
 */
fun parseEpoch(value: Any?): Long? =
    when (value) {
        is Number -> value.toLong()
        is String ->
            runCatching {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(value)?.time
            }.getOrNull()
        else -> null
    }
