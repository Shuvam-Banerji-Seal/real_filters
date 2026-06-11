package com.realfilters.app

import com.realfilters.app.ui.screens.ThemeMode
import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `ThemeMode enum has all three values`() {
        assertEquals(3, ThemeMode.entries.size)
        assertNotNull(ThemeMode.LIGHT)
        assertNotNull(ThemeMode.DARK)
        assertNotNull(ThemeMode.SYSTEM)
    }

    @Test
    fun `fromString returns LIGHT for LIGHT`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    }

    @Test
    fun `fromString returns DARK for DARK`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
    }

    @Test
    fun `fromString returns SYSTEM for SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
    }

    @Test
    fun `fromString returns SYSTEM for null`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(null))
    }

    @Test
    fun `fromString returns SYSTEM for unknown value`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString returns SYSTEM for empty string`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun `fromString is case sensitive`() {
        // Should be case-sensitive - only all-caps accepted
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("dark"))
    }

    @Test
    fun `name field is uppercase for serialization`() {
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
    }

    @Test
    fun `fromString roundtrip preserves value`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromString(mode.name))
        }
    }
}
