package com.alfredJenny.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

enum class CalendarSource { LOCAL, GOOGLE }

data class CalendarInfo(
    val id: String,
    val name: String,
    val accountName: String,
    val source: CalendarSource = CalendarSource.LOCAL,
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val startMs: Long,
    val endMs: Long,
    val calendarName: String,
    val source: CalendarSource = CalendarSource.LOCAL,
)

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val googleService = GoogleCalendarService()

    init {
        // Restore previous Google Sign-In session if still valid
        googleService.tryRestoreFromLastSignIn(context)
    }

    // ── Google Calendar session management ────────────────────────────────────

    fun getGoogleSignInIntent(): Intent = googleService.getSignInIntent(context)

    fun initGoogleCalendar(account: GoogleSignInAccount) {
        googleService.init(context, account)
    }

    fun disconnectGoogle() {
        googleService.signOut(context)
    }

    fun getGoogleEmail(): String? = googleService.getSignedInEmail()

    // ── Calendar list ─────────────────────────────────────────────────────────

    suspend fun getAvailableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        val local = mutableListOf<CalendarInfo>()
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
                    local.add(
                        CalendarInfo(
                            id = cursor.getLong(0).toString(),
                            name = cursor.getString(1) ?: "",
                            accountName = cursor.getString(2) ?: "",
                            source = CalendarSource.LOCAL,
                        )
                    )
                }
            }
        }
        val google = googleService.getCalendars()
        local + google
    }

    // ── Events ────────────────────────────────────────────────────────────────

    suspend fun insertEvent(
        calendarId: String,
        title: String,
        description: String,
        startMs: Long,
        endMs: Long,
    ): String = withContext(Dispatchers.IO) {
        if (calendarId.startsWith("google:")) {
            return@withContext googleService.insertEvent(calendarId, title, description, startMs, endMs)
        }
        // Local calendar
        val localId = calendarId.toLongOrNull() ?: return@withContext ""
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, localId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        uri?.lastPathSegment ?: ""
    }

    suspend fun getEvents(calendarId: String, startMs: Long, endMs: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (calendarId.startsWith("google:")) {
                return@withContext googleService.getEvents(calendarId, startMs, endMs)
            }
            // Local calendar
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
            val args = arrayOf(calendarId, startMs.toString(), endMs.toString())
            runCatching {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection, selection, args,
                    "${CalendarContract.Events.DTSTART} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        events.add(
                            CalendarEvent(
                                id = cursor.getLong(0).toString(),
                                title = cursor.getString(1) ?: "(senza titolo)",
                                description = cursor.getString(2) ?: "",
                                startMs = cursor.getLong(3),
                                endMs = cursor.getLong(4),
                                calendarName = "",
                                source = CalendarSource.LOCAL,
                            )
                        )
                    }
                }
            }
            events
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            val icon = if (ev.source == CalendarSource.GOOGLE) "📅" else "📔"
            "$icon ${ev.title} ($start–$end)"
        }
    }
}
