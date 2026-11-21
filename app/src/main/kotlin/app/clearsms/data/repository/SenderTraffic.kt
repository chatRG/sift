package app.clearsms.data.repository

import app.clearsms.data.db.MessageDao
import app.clearsms.domain.categorizer.DltClass
import app.clearsms.domain.categorizer.DltHeaderParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read model answering one India-specific safety question: does this sender
 * also carry transactional traffic?
 *
 * A sender's route variants collapse to one normalized key - `HDFCBK-P`
 * (promotional) and `HDFCBK-T` (transactional) are the same key - so filing
 * "HDFCBK" as spam also files its OTPs and debit alerts. The categorizer's
 * invariants still lift those back out, but the UI must say so instead of
 * pretending the decision was narrow.
 *
 * Bounded by the number of route variants actually seen (typically <= 4), so
 * one indexed query per report is cheap.
 */
@Singleton
class SenderTraffic
    @Inject
    constructor(
        private val messageDao: MessageDao,
    ) {
        suspend fun hasMixedDltClasses(senderCore: String): Boolean {
            val classes =
                messageDao
                    .rawSendersForCore(senderCore)
                    .mapNotNull { DltHeaderParser.parse(it)?.dltClass }
                    .toSet()
            return DltClass.PROMOTIONAL in classes && classes.any { it != DltClass.PROMOTIONAL }
        }
    }
