package com.passwordassistant.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object GroupVisuals {
    val icons: List<Pair<String, ImageVector>> = listOf(
        "lock" to Icons.Outlined.Lock,
        "terminal" to Icons.Outlined.Terminal,
        "chat" to Icons.AutoMirrored.Outlined.Chat,
        "mail" to Icons.Outlined.Email,
        "card" to Icons.Outlined.CreditCard,
        "wifi" to Icons.Outlined.Wifi,
        "cloud" to Icons.Outlined.Cloud,
        "globe" to Icons.Outlined.Public,
        "fingerprint" to Icons.Outlined.Fingerprint,
        "key" to Icons.Outlined.VpnKey,
        "person" to Icons.Outlined.Person,
        "phone" to Icons.Outlined.Smartphone,
        "game" to Icons.Outlined.Gamepad,
        "shopping" to Icons.Outlined.ShoppingCart,
        "book" to Icons.AutoMirrored.Outlined.MenuBook,
        "folder" to Icons.Outlined.Folder,
    )

    fun iconOf(name: String): ImageVector =
        icons.firstOrNull { it.first == name }?.second ?: Icons.Outlined.Folder

    val colors: List<Color> = listOf(
        Color(0xFFE53935),
        Color(0xFFD81B60),
        Color(0xFF8E24AA),
        Color(0xFF5E35B1),
        Color(0xFF3949AB),
        Color(0xFF1E88E5),
        Color(0xFF00897B),
        Color(0xFF43A047),
        Color(0xFFF4511E),
        Color(0xFF6D4C41),
        Color(0xFF546E7A),
        Color(0xFFFDD835),
    )

    fun colorOf(index: Int): Color =
        colors[((index % colors.size) + colors.size) % colors.size]
}
