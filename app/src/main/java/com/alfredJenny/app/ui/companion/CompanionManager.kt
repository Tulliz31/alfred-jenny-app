package com.alfredJenny.app.ui.companion

import androidx.compose.ui.graphics.Color

enum class AvatarType { SPRITE, PUPPET }

data class CompanionConfig(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val primaryColorLight: Color,
    val avatarType: AvatarType,
    /** Greeting shown on the splash screen after login. */
    val welcomeMessage: String,
)

object CompanionManager {

    val ALFRED = CompanionConfig(
        id = "alfred",
        name = "Alfred",
        primaryColor = Color(0xFF1A3A5C),
        primaryColorLight = Color(0xFF5B9BD5),
        avatarType = AvatarType.SPRITE,
        welcomeMessage = "Bentornato! Alfred è pronto.",
    )

    val JENNY = CompanionConfig(
        id = "jenny",
        name = "Jenny",
        primaryColor = Color(0xFF5C1A3A),
        primaryColorLight = Color(0xFFD55B9B),
        avatarType = AvatarType.PUPPET,
        welcomeMessage = "Bentornata! Jenny è qui.",
    )

    fun getActiveCompanion(role: String): CompanionConfig =
        if (role == "admin") JENNY else ALFRED
}
