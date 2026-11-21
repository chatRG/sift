package app.clearsms.domain.categorizer

import app.clearsms.data.repository.SenderNormalizer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The TRAI route prefix / DLT class split, and its agreement with [SenderNormalizer]. */
class DltHeaderParserTest {
    @Test
    fun `parses route prefix, header and promotional suffix`() {
        val parsed = DltHeaderParser.parse("VM-HDFCBK-P")
        assertThat(parsed).isEqualTo(DltHeader(routePrefix = "VM", header = "HDFCBK", dltClass = DltClass.PROMOTIONAL))
    }

    @Test
    fun `every DLT class is recognised`() {
        assertThat(DltHeaderParser.parse("HDFCBK-T")?.dltClass).isEqualTo(DltClass.TRANSACTIONAL)
        assertThat(DltHeaderParser.parse("HDFCBK-S")?.dltClass).isEqualTo(DltClass.SERVICE)
        assertThat(DltHeaderParser.parse("HDFCBK-G")?.dltClass).isEqualTo(DltClass.GOVERNMENT)
    }

    @Test
    fun `a header with no suffix keeps a null class`() {
        val parsed = DltHeaderParser.parse("AD-HDFCBK")
        assertThat(parsed).isEqualTo(DltHeader(routePrefix = "AD", header = "HDFCBK", dltClass = null))
    }

    @Test
    fun `phone numbers, short codes and blank input are not A2P headers`() {
        assertThat(DltHeaderParser.parse("+91 98765 43210")).isNull()
        assertThat(DltHeaderParser.parse("9876543210")).isNull()
        assertThat(DltHeaderParser.parse("56767")).isNull()
        assertThat(DltHeaderParser.parse("   ")).isNull()
    }

    @Test
    fun `transactional classes are the hard veto, promotional is not`() {
        assertThat(DltHeaderParser.isTransactional("VM-HDFCBK-T")).isTrue()
        assertThat(DltHeaderParser.isTransactional("HDFCBK-S")).isTrue()
        assertThat(DltHeaderParser.isTransactional("HDFCBK-G")).isTrue()
        assertThat(DltHeaderParser.isTransactional("VM-HDFCBK-P")).isFalse()
        assertThat(DltHeaderParser.isTransactional("AD-HDFCBK")).isFalse()
    }

    @Test
    fun `header always equals the thread key the rest of the app uses`() {
        val corpus =
            listOf(
                "VM-HDFCBK-P",
                "AD-HDFCBK",
                "HDFCBK",
                "vm-hdfcbk-t",
                "AD-ICICIB-S",
                "VK-SBIINB-G",
            )
        for (sender in corpus) {
            // The two rules parse independently; a drift would split a
            // sender's thread key from its DLT class.
            assertThat(DltHeaderParser.parse(sender)?.header).isEqualTo(SenderNormalizer.normalize(sender))
        }
    }
}
