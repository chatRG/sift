package app.clearsms.domain.parser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The bulk-marketing heuristic's two-lexicon shape and its hard vetoes. */
class SpamHeuristicTest {
    private val heuristic = SpamHeuristic()

    private val pitch =
        "Pre-approved loan of Rs 5,00,000. Apply now at https://loans.example/apply " +
            "or call 1800123456. T&C apply. Unsubscribe to opt out."

    @Test
    fun `bulk commercial pitch with a way to act is spam`() {
        assertThat(heuristic.isSpam("AD-LOANCO-P", pitch, isContact = false)).isTrue()
    }

    @Test
    fun `a contact is never heuristically spammed`() {
        assertThat(heuristic.isSpam("AD-LOANCO-P", pitch, isContact = true)).isFalse()
    }

    @Test
    fun `transactional, service and government headers are a hard veto`() {
        // The same body from a bank's -T header must stay in the inbox: the
        // suffix is the network's own declaration of transactional traffic.
        assertThat(heuristic.isSpam("VM-HDFCBK-T", pitch, isContact = false)).isFalse()
        assertThat(heuristic.isSpam("HDFCBK-S", pitch, isContact = false)).isFalse()
        assertThat(heuristic.isSpam("HDFCBK-G", pitch, isContact = false)).isFalse()
    }

    @Test
    fun `promotional suffix alone is not proof - the content still has to qualify`() {
        assertThat(heuristic.isSpam("VM-HDFCBK-P", "Your OTP is 482913 for txn of INR 1200", isContact = false)).isFalse()
    }

    @Test
    fun `a commercial message without a way to act is not spam`() {
        assertThat(
            heuristic.isSpam(
                "AD-LOANCO-P",
                "We sell insurance and mutual funds. Speak to your branch.",
                isContact = false,
            ),
        ).isFalse()
    }

    @Test
    fun `ordinary conversation is never spam`() {
        assertThat(heuristic.isSpam("+91 98765 43210", "Are we still meeting at 7?", isContact = false)).isFalse()
    }

    @Test
    fun `financial evidence vetoes the heuristic`() {
        val statement =
            "Pre-approved loan offer. Your statement for folio 12345678 has units allotted " +
                "and NAV updated. Apply now at https://loans.example/apply"
        assertThat(heuristic.isSpam("AD-LOANCO-P", statement, isContact = false)).isFalse()
    }
}
