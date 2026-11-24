package com.plantkeeper.ui

import com.plantkeeper.data.QuantityUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SeedFormTest {
    private fun err(name: String = "Tomaat", qty: String = "2", year: String = "2027", inR: Pair<Int?, Int?> = null to null, outR: Pair<Int?, Int?> = null to null) =
        seedFormError(name, qty, year, inR, outR)

    @Test
    fun validInput() {
        assertNull(err())
        assertNull(err(qty = "1,5", year = "", inR = 10 to 2))
        assertNull(err(qty = ""))
    }

    @Test
    fun invalidInput() {
        assertNotNull(err(name = " "))
        assertNotNull(err(qty = "twee"))
        assertNotNull(err(year = "27x"))
        assertNotNull(err(inR = 3 to null))
        assertNotNull(err(outR = null to 5))
    }

    @Test
    fun quantityText() {
        assertEquals("1 zakje", qtyText(1.0, QuantityUnit.ZAKJE))
        assertEquals("4 bussen", qtyText(4.0, QuantityUnit.BUS))
        assertEquals("1 bol", qtyText(1.0, QuantityUnit.BOLLEN))
        assertEquals("1,5 zakjes", qtyText(1.5, QuantityUnit.ZAKJE))
        assertEquals(1.5, parseNumber(" 1,5 "))
    }
}
