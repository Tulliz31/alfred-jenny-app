package com.alfredJenny.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarInfo(val id: Long, val name: String, val accountName: String)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String,
    val startMs: Long,
    val endMs: Long,
    val calendarName: String,
)

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getAvailableCalendars(): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    calendars.add(
                        CalendarInfo(
                            id = cursor.getLong(0),
                            name = cursor.getString(1) ?: "",
                            accountName = cursor.getString(2) ?: "",
                        )
                    )
                }
            }
        }
        return calendars
    }

    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        description: String,
        startMs: Long,
        endMs: Long,
    ): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        uri?.lastPathSegment?.toLongOrNull() ?: -1L
    }

    suspend fun getEvents(calendarId: Long, startMs: Long, endMs: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val events = mutableListOf<CalendarEvent>()
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
            )
            val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                    "${CalendarContract.Events.DTSTART} >= ? AND " +
                    "${CalendarContract.Events.DTSTART} <= ?"
            val args = arrayOf(calendarId.toString(), startMs.toString(), endMs.toString())
            runCatching {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection, selection, args,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        events.add(
                            CalendarEvent(
                                id = cursor.getLong(0),
                                title = cursor.getString(1) ?: "(senza titolo)",
                                description = cursor.getString(2) ?: "",
                                startMs = cursor.getLong(3),
                                endMs = cursor.getLong(4),
                                calendarName = "",
                            )
                        )
                    }
                }
            }
            events
        }

    /** Parse ISO date + time strings into milliseconds since epoch. */
    fun parseEventTimeMs(date: String, time: String): Long {
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            fmt.timeZone = TimeZone.getDefault()
            fmt.parse("$date $time")?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }

    fun formatEventsForDisplay(events: List<CalendarEvent>): String {
        if (events.isEmpty()) return "Nessun evento trovato."
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return events.joinToString("\n") { ev ->
            val start = timeFmt.format(ev.startMs)
            val end = timeFmt.format(ev.endMs)
            "• ${ev.title} ($start–$end)"
        }
    }
}
