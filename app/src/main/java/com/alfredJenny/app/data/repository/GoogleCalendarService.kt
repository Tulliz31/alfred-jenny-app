package com.alfredJenny.app.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin wrapper around Google Calendar API v3. Owned by [CalendarRepository]. */
internal class GoogleCalendarService(private val appName: String = "AlfredJenny") {

    private var service: Calendar? = null
    private var _email: String? = null

    fun isConnected(): Boolean = service != null
    fun getSignedInEmail(): String? = _email

    // ── Sign-in intent ────────────────────────────────────────────────────────

    fun getSignInIntent(context: Context): Intent =
        buildGoogleClient(context).signInIntent

    // ── Session management ────────────────────────────────────────────────────

    fun init(context: Context, account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(CalendarScopes.CALENDAR))
            .apply {
                backOff = ExponentialBackOff()
                selectedAccount = account.account
            }
        service = Calendar.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(appName).build()
        _email = account.email
    }

    /** Returns the signed-in email if a valid session was restored, null otherwise. */
    fun tryRestoreFromLastSignIn(context: Context): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasScope = GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))
        if (!hasScope) return null
        init(context, account)
        return account.email
    }

    fun signOut(context: Context) {
        buildGoogleClient(context).signOut()
        service = null
        _email = null
    }

    // ── Calendar API calls ────────────────────────────────────────────────────

    suspend fun getCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        val svc = service ?: return@withContext emptyList()
        runCatching {
            svc.calendarList().list().execute().items.orEmpty().map { item ->
                CalendarInfo(
                    id = "google:${item.id}",
                    name = item.summary ?: item.id ?: "(senza nome)",
                    accountName = _email ?: "",
                    source = CalendarSource.GOOGLE,
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getEvents(calendarId: String, startMs: Long, endMs: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val svc = service ?: return@withContext emptyList()
            val rawId = calendarId.removePrefix("google:")
            runCatching {
                val timeMin = com.google.api.client.util.DateTime(startMs)
                val timeMax = com.google.api.client.util.DateTime(endMs)
                svc.events().list(rawId)
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()
                    .items.orEmpty().map { event ->
                        CalendarEvent(
                            id = event.id ?: "",
                            title = event.summary ?: "(senza titolo)",
                            description = event.description ?: "",
                            startMs = event.start?.dateTime?.value
                                ?: event.start?.date?.value ?: startMs,
                            endMs = event.end?.dateTime?.value
                                ?: event.end?.date?.value ?: endMs,
                            calendarName = rawId,
                            source = CalendarSource.GOOGLE,
                        )
                    }
            }.getOrDefault(emptyList())
        }

    suspend fun insertEvent(
        calendarId: String,
        title: String,
        description: String,
        startMs: Long,
        endMs: Long,
    ): String = withContext(Dispatchers.IO) {
        val svc = service ?: return@withContext ""
        val rawId = calendarId.removePrefix("google:")
        val tz = java.util.TimeZone.getDefault().id
        runCatching {
            val event = Event().apply {
                summary = title
                this.description = description
                start = EventDateTime()
                    .setDateTime(com.google.api.client.util.DateTime(startMs))
                    .setTimeZone(tz)
                end = EventDateTime()
                    .setDateTime(com.google.api.client.util.DateTime(endMs))
                    .setTimeZone(tz)
            }
            svc.events().insert(rawId, event).execute().id ?: ""
        }.getOrDefault("")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildGoogleClient(context: Context) =
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(CalendarScopes.CALENDAR))
                .build()
        )
}
