package com.termkey.ime

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
    fun getPalette(themeKey: String?): KeyboardColorPalette {
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
                surfaceColor = 0xFFF0F0F0.toInt(),
                surface2Color = 0xFFE0E0E0.toInt(),
                borderColor = 0xFFCCCCCC.toInt(),
                textColor = 0xFF222222.toInt(),
                textDimColor = 0xFF666666.toInt(),
                accentColor = 0xFF007AFF.toInt(),
                keyBgColor = 0xFFFFFFFF.toInt(),
                keyAltBgColor = 0xFFEAEAEA.toInt(),
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
