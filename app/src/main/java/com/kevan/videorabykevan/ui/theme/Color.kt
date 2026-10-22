package com.kevan.videorabykevan.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Deep Onyx & Obsidian Luxury Studio Palette
// ==========================================
val DarkBackground = Color(0xFF090A0F)       // Absolute Obsidian
val DarkSurface = Color(0xFF12141F)          // Deep Slate Studio Surface
val DarkSurfaceVariant = Color(0xFF1A1D2B)   // Elevated Glass Pane
val DarkCard = Color(0xFF202436)             // High-End Card Container
val DarkElevated = Color(0xFF282D42)         // Active / Elevated Element

// ==========================================
// Luxury Brand Accents
// ==========================================
val GoldChampagne = Color(0xFFF7D070)        // Signature Champagne Gold
val GoldMetallic = Color(0xFFD4AF37)         // Polished Studio Gold
val GoldGlow = Color(0xFFFFE599)             // Radiant Highlight
val AmberWarm = Color(0xFFFFB300)

val RoyalAmethyst = Color(0xFF8A2BE2)        // Electric Violet
val ElectricPurple = Color(0xFFA855F7)
val NeonCoral = Color(0xFFFF2A85)            // Hot TikTok / Reels Pink
val HyperCyan = Color(0xFF00F2FE)            // Crisp Studio Cyan
val EmeraldAccent = Color(0xFF00E676)        // Offline / Active Signal Green

// Legacy Aliases for backwards compatibility
val AccentCyan = HyperCyan
val AccentPurple = RoyalAmethyst
val AccentPink = NeonCoral
val AccentYellow = GoldChampagne

// ==========================================
// Text & Borders
// ==========================================
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B5C6)
val TextTertiary = Color(0xFF6B7288)
val BorderSubtle = Color(0xFF2E3245)
val GlassBorder = Color(0x33FFFFFF)
val GoldBorder = Color(0x55F7D070)

// Safe Zone Guides
val SafeZoneWarningRed = Color(0x99FF3B30)
val SafeZoneGuideGreen = Color(0x6634C759)
val SafeZoneGuideCyan = Color(0x8800F2FE)

// ==========================================
// Luxury Brand Gradients
// ==========================================
val LuxuryGoldGradient = Brush.linearGradient(
    listOf(Color(0xFFFFF3C4), GoldChampagne, GoldMetallic, Color(0xFF996515))
)

val LuxuryAmethystGradient = Brush.linearGradient(
    listOf(RoyalAmethyst, NeonCoral)
)

val HyperStreamGradient = Brush.linearGradient(
    listOf(HyperCyan, RoyalAmethyst)
)

val DarkStudioCardGradient = Brush.verticalGradient(
    listOf(Color(0xFF1F2233), Color(0xFF141624))
)

val GlassHighlightGradient = Brush.verticalGradient(
    listOf(Color(0x2EFFFFFF), Color(0x0AFFFFFF))
)

val GoldRimGradient = Brush.linearGradient(
    listOf(Color(0x66F7D070), Color(0x228A2BE2), Color(0x1100F2FE))
)
