package com.termkey.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KeyboardThemeTest {

    @Test
    fun testGetPalette() {
        val paletteGreen = KeyboardTheme.getPalette("terminal_green")
        assertNotNull(paletteGreen)

        val paletteDark = KeyboardTheme.getPalette("classic_dark")
        assertNotNull(paletteDark)

        val paletteOled = KeyboardTheme.getPalette("oled_black")
        assertNotNull(paletteOled)

        val paletteDracula = KeyboardTheme.getPalette("dracula")
        assertNotNull(paletteDracula)

        val paletteNord = KeyboardTheme.getPalette("nord")
        assertNotNull(paletteNord)

        val paletteGruvbox = KeyboardTheme.getPalette("gruvbox")
        assertNotNull(paletteGruvbox)

        val paletteSolarized = KeyboardTheme.getPalette("solarized_dark")
        assertNotNull(paletteSolarized)

        val paletteDefault = KeyboardTheme.getPalette("unknown")
        assertNotNull(paletteDefault)
    }

    @Test
    fun testHexColorParser() {
        val color1 = KeyboardTheme.parseHexColor("#FF0000", 0)
        assertEquals(0xFFFF0000.toInt(), color1)

        val color2 = KeyboardTheme.parseHexColor("00FF00", 0)
        assertEquals(0xFF00FF00.toInt(), color2)

        val colorInvalid = KeyboardTheme.parseHexColor("invalid", 123)
        assertEquals(123, colorInvalid)
    }
}
