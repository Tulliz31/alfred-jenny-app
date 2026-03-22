package com.alfredJenny.app.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

enum class PermissionNeeded { NONE, CALENDAR, NOTIFICATION }

object PermissionUtils {

    val CALENDAR_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
    )

    val NOTIFICATION_PERMISSIONS: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else emptyArray()

    val IMAGE_PERMISSIONS: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
                PermissionChecker.PERMISSION_GRANTED

    fun areAllGranted(context: Context, permissions: Array<String>): Boolean =
        permissions.all { isGranted(context, it) }

    fun areCalendarGranted(context: Context) =
        areAllGranted(context, CALENDAR_PERMISSIONS)

    fun areNotificationsGranted(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                isGranted(context, Manifest.permission.POST_NOTIFICATIONS)

    fun areImagesGranted(context: Context) =
        areAllGranted(context, IMAGE_PERMISSIONS)

    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
