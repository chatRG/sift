package app.clearsms.testing

import app.clearsms.data.repository.SpamReport
import app.clearsms.data.repository.SpamReporting

/** In-memory [SpamReporting] that records calls and returns fixed outcomes. */
class FakeSpamReporting(
    var reportResult: SpamReport? = null,
    var notSpamResult: Int = 0,
    var filedAsSpam: Boolean = false,
) : SpamReporting {
    val reported = mutableListOf<String>()
    val cleared = mutableListOf<String>()

    override suspend fun report(sender: String): SpamReport? {
        reported += sender
        return reportResult
    }

    override suspend fun notSpam(sender: String): Int {
        cleared += sender
        return notSpamResult
    }

    override suspend fun isFiledAsSpam(sender: String): Boolean = filedAsSpam
}
