package com.xptlabs.varliktakibi

import com.xptlabs.varliktakibi.core.ext.searchMatches
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    @Test
    fun `turkce i ve aksanlar esit sayilir`() {
        assertTrue("İŞ PORTFÖY PARA PİYASASI".searchMatches("is portfoy"))
        assertTrue("Bitcoin".searchMatches("Bıt"))
        assertTrue("BITCOIN".searchMatches("bit"))
        assertTrue("Ripple".searchMatches("RIP"))
        assertTrue("Çeyrek Altın".searchMatches("ceyrek"))
        assertTrue("Gram Altın".searchMatches(""))
        assertFalse("Gram Altın".searchMatches("gümüş"))
    }
}
