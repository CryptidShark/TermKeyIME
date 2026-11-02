package com.termkey.ime

import android.content.SharedPreferences

data class KeyboardColorPalette(
    val surfaceColor: Int,
    val surface2Color: Int,
    val borderColor: Int,
    val textColor: Int,
    val textDimColor: Int,
    val accentColor: Int,
    val keyBgColor: Int,
    val keyAltBgColor: Int,
    val keyActiveColor: Int
)

object KeyboardTheme {
    fun parseHexColor(hexStr: String?, defaultColor: Int): Int {
        if (hexStr.isNullOrBlank()) return defaultColor
        return try {
            val cleanHex = hexStr.trim().removePrefix("#")
            val fullHex = when (cleanHex.length) {
                6 -> "FF$cleanHex"
                8 -> cleanHex
                3 -> "FF" + cleanHex.map { "$it$it" }.joinToString("")
                else -> return defaultColor
            }
            fullHex.toLong(16).toInt()
        } catch (_: Exception) {
            defaultColor
        }
    }

    @JvmOverloads
    fun getPalette(themeKey: String?, prefs: SharedPreferences? = null): KeyboardColorPalette {
        if (themeKey == "custom" && prefs != null) {
            val surface = parseHexColor(prefs.getString("custom_surface_color", "#0D1117"), 0xFF0D1117.toInt())
            val keyBg = parseHexColor(prefs.getString("custom_key_bg_color", "#21262D"), 0xFF21262D.toInt())
            val text = parseHexColor(prefs.getString("custom_text_color", "#FFFFFF"), 0xFFFFFFFF.toInt())
            val accent = parseHexColor(prefs.getString("custom_accent_color", "#00FF66"), 0xFF00FF66.toInt())
            return KeyboardColorPalette(
                surfaceColor = surface,
                surface2Color = keyBg,
                borderColor = keyBg,
                textColor = text,
                textDimColor = text,
                accentColor = accent,
                keyBgColor = keyBg,
                keyAltBgColor = keyBg,
                keyActiveColor = accent,
            )
        }

        return when (themeKey) {
            "classic_dark" -> KeyboardColorPalette(
                surfaceColor = 0xFF121212.toInt(),
                surface2Color = 0xFF1E1E1E.toInt(),
                borderColor = 0xFF333333.toInt(),
                textColor = 0xFFFFFFFF.toInt(),
                textDimColor = 0xFFAAAAAA.toInt(),
                accentColor = 0xFF33B5E5.toInt(),
                keyBgColor = 0xFF2A2A2A.toInt(),
                keyAltBgColor = 0xFF333333.toInt(),
                keyActiveColor = 0xFF33B5E5.toInt()
            )
            "oled_black" -> KeyboardColorPalette(
                surfaceColor = 0xFF000000.toInt(),
                surface2Color = 0xFF121212.toInt(),
                borderColor = 0xFF222222.toInt(),
                textColor = 0xFFFFFFFF.toInt(),
                textDimColor = 0xFF888888.toInt(),
                accentColor = 0xFF00E5FF.toInt(),
                keyBgColor = 0xFF161616.toInt(),
                keyAltBgColor = 0xFF222222.toInt(),
                keyActiveColor = 0xFF00E5FF.toInt()
            )
            "dracula" -> KeyboardColorPalette(
                surfaceColor = 0xFF282A36.toInt(),
                surface2Color = 0xFF343746.toInt(),
                borderColor = 0xFF44475A.toInt(),
                textColor = 0xFFF8F8F2.toInt(),
                textDimColor = 0xFF6272A4.toInt(),
                accentColor = 0xFFFF79C6.toInt(),
                keyBgColor = 0xFF44475A.toInt(),
                keyAltBgColor = 0xFF6272A4.toInt(),
                keyActiveColor = 0xFFFF79C6.toInt()
            )
            "nord" -> KeyboardColorPalette(
                surfaceColor = 0xFF2E3440.toInt(),
                surface2Color = 0xFF3B4252.toInt(),
                borderColor = 0xFF434C5E.toInt(),
                textColor = 0xFFECEFF4.toInt(),
                textDimColor = 0xFFD8DEE9.toInt(),
                accentColor = 0xFF88C0D0.toInt(),
                keyBgColor = 0xFF3B4252.toInt(),
                keyAltBgColor = 0xFF434C5E.toInt(),
                keyActiveColor = 0xFF88C0D0.toInt()
            )
            "gruvbox" -> KeyboardColorPalette(
                surfaceColor = 0xFF282828.toInt(),
                surface2Color = 0xFF3C3836.toInt(),
                borderColor = 0xFF504945.toInt(),
                textColor = 0xFFEBDBB2.toInt(),
                textDimColor = 0xFFA89984.toInt(),
                accentColor = 0xFFFABD2F.toInt(),
                keyBgColor = 0xFF3C3836.toInt(),
                keyAltBgColor = 0xFF504945.toInt(),
                keyActiveColor = 0xFFFABD2F.toInt()
            )
            "solarized_dark" -> KeyboardColorPalette(
                surfaceColor = 0xFF002B36.toInt(),
                surface2Color = 0xFF073642.toInt(),
                borderColor = 0xFF586E75.toInt(),
                textColor = 0xFF93A1A1.toInt(),
                textDimColor = 0xFF657B83.toInt(),
                accentColor = 0xFF2AA198.toInt(),
                keyBgColor = 0xFF073642.toInt(),
                keyAltBgColor = 0xFF586E75.toInt(),
                keyActiveColor = 0xFF2AA198.toInt()
            )
            "terminal_amber" -> KeyboardColorPalette(
                surfaceColor = 0xFF120F0A.toInt(),
                surface2Color = 0xFF221B12.toInt(),
                borderColor = 0xFF44331A.toInt(),
                textColor = 0xFFFFDFA0.toInt(),
                textDimColor = 0xFFAA9060.toInt(),
                accentColor = 0xFFFFB000.toInt(),
                keyBgColor = 0xFF282015.toInt(),
                keyAltBgColor = 0xFF342A1C.toInt(),
                keyActiveColor = 0xFFFFB000.toInt()
            )
            "cyberpunk" -> KeyboardColorPalette(
                surfaceColor = 0xFF150E1F.toInt(),
                surface2Color = 0xFF231834.toInt(),
                borderColor = 0xFF45266B.toInt(),
                textColor = 0xFFF0E6FF.toInt(),
                textDimColor = 0xFFA892CC.toInt(),
                accentColor = 0xFF00F0FF.toInt(),
                keyBgColor = 0xFF2C1D42.toInt(),
                keyAltBgColor = 0xFF392455.toInt(),
                keyActiveColor = 0xFF00F0FF.toInt()
            )
            "light" -> KeyboardColorPalette(
                surfaceColor = 0xFFF8F9FA.toInt(),
                surface2Color = 0xFFE9ECEF.toInt(),
                borderColor = 0xFFDEE2E6.toInt(),
                textColor = 0xFF212529.toInt(),
                textDimColor = 0xFF6C757D.toInt(),
                accentColor = 0xFF007AFF.toInt(),
                keyBgColor = 0xFFFFFFFF.toInt(),
                keyAltBgColor = 0xFFE9ECEF.toInt(),
                keyActiveColor = 0xFF007AFF.toInt()
            )
            "crimson" -> KeyboardColorPalette(
                surfaceColor = 0xFF1A0F0F.toInt(),
                surface2Color = 0xFF2A1818.toInt(),
                borderColor = 0xFF4D2222.toInt(),
                textColor = 0xFFFFE0E0.toInt(),
                textDimColor = 0xFFCCA0A0.toInt(),
                accentColor = 0xFFFF3333.toInt(),
                keyBgColor = 0xFF331C1C.toInt(),
                keyAltBgColor = 0xFF422525.toInt(),
                keyActiveColor = 0xFFFF3333.toInt()
            )
            "dynamic_system" -> KeyboardColorPalette(
                surfaceColor = 0xFF181A20.toInt(),
                surface2Color = 0xFF23262F.toInt(),
                borderColor = 0xFF353945.toInt(),
                textColor = 0xFFF4F5F6.toInt(),
                textDimColor = 0xFF8C9097.toInt(),
                accentColor = 0xFF6C5CE7.toInt(),
                keyBgColor = 0xFF2B2F3A.toInt(),
                keyAltBgColor = 0xFF353945.toInt(),
                keyActiveColor = 0xFF6C5CE7.toInt()
            )
            else -> { // terminal_green (default)
                KeyboardColorPalette(
                    surfaceColor = 0xFF0D1117.toInt(),
                    surface2Color = 0xFF161B22.toInt(),
                    borderColor = 0xFF30363D.toInt(),
                    textColor = 0xFFC9D1D9.toInt(),
                    textDimColor = 0xFF8B949E.toInt(),
                    accentColor = 0xFF00FF66.toInt(),
                    keyBgColor = 0xFF21262D.toInt(),
                    keyAltBgColor = 0xFF30363D.toInt(),
                    keyActiveColor = 0xFF00FF66.toInt()
                )
            }
        }
    }
}
