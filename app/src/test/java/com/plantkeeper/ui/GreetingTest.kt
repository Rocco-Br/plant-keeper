package com.plantkeeper.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun `dagdelen met hun grenzen`() {
        assertEquals("Goedenacht, Rocco", greeting(0, "Rocco"))
        assertEquals("Goedenacht, Rocco", greeting(5, "Rocco"))
        assertEquals("Goedemorgen, Rocco", greeting(6, "Rocco"))
        assertEquals("Goedemorgen, Rocco", greeting(11, "Rocco"))
        assertEquals("Goedemiddag, Rocco", greeting(12, "Rocco"))
        assertEquals("Goedemiddag, Rocco", greeting(17, "Rocco"))
        assertEquals("Goedenavond, Rocco", greeting(18, "Rocco"))
        assertEquals("Goedenavond, Rocco", greeting(23, "Rocco"))
    }

    @Test
    fun `zonder naam geen komma`() {
        assertEquals("Goedemorgen", greeting(9, ""))
        assertEquals("Goedemorgen", greeting(9, null))
        assertEquals("Goedemorgen", greeting(9, "  "))
        assertEquals("Goedemorgen, Rocco", greeting(9, " Rocco "))
    }
}
