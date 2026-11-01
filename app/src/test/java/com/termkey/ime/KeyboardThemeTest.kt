package com.termkey.ime

import org.junit.Assert.assertNotNull
import org.junit.Test

class KeyboardThemeTest {

    @Test
    fun testGetPalette() {
        val paletteGreen = KeyboardTheme.getPalette("terminal_green")
        assertNotNull(paletteGreen)

        val paletteDark = KeyboardTheme.getPalette("classic_dark")
        assertNotNull(paletteDark)

        val paletteAmber = KeyboardTheme.getPalette("terminal_amber")
        assertNotNull(paletteAmber)

        val paletteDefault = KeyboardTheme.getPalette("unknown")
        assertNotNull(paletteDefault)
    }
}
