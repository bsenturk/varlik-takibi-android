package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.ui.paywall.FeatureGatePaywall
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGatePaywallTest {

    @Test
    fun `ilk dokunus acar, 15 dk icindekiler yutulur, sonra yeniden acar`() {
        FeatureGatePaywall.resetForTest()
        val t0 = 1_000_000L
        assertTrue(FeatureGatePaywall.shouldShow(t0))
        assertFalse(FeatureGatePaywall.shouldShow(t0 + 1_000))
        assertFalse(FeatureGatePaywall.shouldShow(t0 + 14 * 60_000))
        assertTrue(FeatureGatePaywall.shouldShow(t0 + 15 * 60_000 + 1))
    }
}
