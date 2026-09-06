package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.review.ASK_AFTER_ASSET
import com.xptlabs.varliktakibi.review.shouldAsk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Puan istemi kapısı. Play'in kotası zaten sınırlıyor ama bir kez sorabildiğimiz
 * anı erken harcamak geri alınamaz, o yüzden kapı burada doğrulanıyor.
 */
class ReviewPromptTest {

    @Test
    fun `esikten once sorulmaz`() {
        for (count in 1 until ASK_AFTER_ASSET) {
            assertFalse("$count. varlıkta sorulmamalı", shouldAsk(count, false, false))
        }
    }

    @Test
    fun `esikte ve sonrasinda sorulur`() {
        assertTrue(shouldAsk(ASK_AFTER_ASSET, false, false))
        assertTrue(shouldAsk(ASK_AFTER_ASSET + 7, false, false))
    }

    @Test
    fun `bir kez soruldiysa tekrar sorulmaz`() {
        assertFalse(shouldAsk(ASK_AFTER_ASSET, true, false))
    }

    @Test
    fun `reklam veya paywall varken ustune binmez`() {
        assertFalse(shouldAsk(ASK_AFTER_ASSET, false, true))
    }
}
