package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF34D399),       // Emerald 400
    secondary = Color(0xFF10B981),     // Emerald 500
    tertiary = Color(0xFFFBBF24),       // Amber 400
    background = Color(0xFF0F172A),     // Slate 950/900 background
    surface = Color(0xFF1E293B),        // Slate 800 cards
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFFF8FAF9),
    onTertiary = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAF9),
    onSurface = Color(0xFFF8FAF9),
    surfaceVariant = Color(0xFF334155),  // Slate 700 hover/badge
    onSurfaceVariant = Color(0xFF94A3B8) // Slate 400 secondary text
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF064E3B),        // Emerald 900
    secondary = Color(0xFF059669),      // Emerald 600
    tertiary = Color(0xFFD4AF37),       // Antique Gold / Brass
    background = Color(0xFFF8FAF9),     // Clear light off-white from design HTML
    surface = Color(0xFFFFFFFF),        // White cards
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF1E293B),
    onBackground = Color(0xFF0F172A),   // Slate 900 primary text
    onSurface = Color(0xFF0F172A),      // Slate 900 primary text
    surfaceVariant = Color(0xFFECFDF5),  // Emerald-50 light tint
    onSurfaceVariant = Color(0xFF047857) // Emerald-700
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force false to preserve the customized brand identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
