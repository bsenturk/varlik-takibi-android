package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.ext.Days
import com.xptlabs.varliktakibi.core.ext.parseTimestampOrNull
import com.xptlabs.varliktakibi.ui.addasset.toDoubleOrNullTr
import com.xptlabs.varliktakibi.ui.common.KeypadInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Gün matematiği ve sayı girişi — grafikler ve para girişi buna dayanıyor. */
class DaysAndInputTest {

    // ── Gün aralıkları ───────────────────────────────────────────────────────

    @Test
    fun `aralik iki ucu da icerir`() {
        val from = Days.of(LocalDate.of(2026, 3, 1))
        val to = Days.of(LocalDate.of(2026, 3, 5))
        assertEquals(5, Days.range(from, to).size)
    }

    @Test
    fun `tek gunluk aralik bir eleman`() {
        val day = Days.of(LocalDate.of(2026, 3, 1))
        assertEquals(listOf(day), Days.range(day, day))
    }

    @Test
    fun `ters aralik bos`() {
        val from = Days.of(LocalDate.of(2026, 3, 5))
        val to = Days.of(LocalDate.of(2026, 3, 1))
        assertTrue(Days.range(from, to).isEmpty())
    }

    @Test
    fun `ay ve yil sinirini asar`() {
        val from = Days.of(LocalDate.of(2025, 12, 30))
        val to = Days.of(LocalDate.of(2026, 1, 2))
        assertEquals(4, Days.range(from, to).size)
    }

    @Test
    fun `gun basi ayni gunun her anini ayni anahtara indirger`() {
        val date = LocalDate.of(2026, 6, 15)
        val morning = date.atTime(3, 0).atZone(Days.zone).toInstant().toEpochMilli()
        val evening = date.atTime(23, 59).atZone(Days.zone).toInstant().toEpochMilli()
        assertEquals(Days.startOf(morning), Days.startOf(evening))
    }

    // ── Zaman damgası ────────────────────────────────────────────────────────

    @Test
    fun `postgrest zaman damgasi cozulur`() {
        // PostgREST her iki biçimi de döndürebiliyor.
        assertNotNull("2026-08-22T10:15:30.123456+00:00".parseTimestampOrNull())
        assertNotNull("2026-08-22T10:15:30Z".parseTimestampOrNull())
    }

    @Test
    fun `bozuk zaman damgasi null doner`() {
        // Tek bozuk satır tüm fiyat listesini düşürmemeli.
        assertNull("dun aksam".parseTimestampOrNull())
    }

    // ── Sayı girişi ──────────────────────────────────────────────────────────

    @Test
    fun `virgullu sayi cozulur`() {
        assertEquals(12.5, "12,5".toDoubleOrNullTr()!!, 1e-9)
        assertEquals(1234.56, "1.234,56".toDoubleOrNullTr()!!, 1e-9)
    }

    @Test
    fun `virgul yokken nokta ondalik sayilir`() {
        // "12.5" yazan kullanıcı 125 elde etmemeli.
        assertEquals(12.5, "12.5".toDoubleOrNullTr()!!, 1e-9)
    }

    @Test
    fun `bos ve gecersiz girdi null`() {
        assertNull("".toDoubleOrNullTr())
        assertNull("abc".toDoubleOrNullTr())
    }

    // ── Tuş takımı ───────────────────────────────────────────────────────────

    @Test
    fun `ondalik hane siniri asilmaz`() {
        assertEquals("1,23", KeypadInput.appendDigit("1,23", "4", maxDecimals = 2))
        assertEquals("1,234", KeypadInput.appendDigit("1,23", "4", maxDecimals = 3))
    }

    @Test
    fun `bastaki sifir yutulur ama ondalikta korunur`() {
        assertEquals("5", KeypadInput.appendDigit("0", "5", maxDecimals = 2))
        assertEquals("0,5", KeypadInput.appendDigit("0,", "5", maxDecimals = 2))
    }

    @Test
    fun `ikinci virgul eklenmez`() {
        assertEquals("1,5", KeypadInput.appendComma("1,5"))
        assertEquals("0,", KeypadInput.appendComma(""))
        assertEquals("12,", KeypadInput.appendComma("12"))
    }

    @Test
    fun `silme bos metinde patlamaz`() {
        assertEquals("", KeypadInput.backspace(""))
        assertEquals("1", KeypadInput.backspace("12"))
    }
}
