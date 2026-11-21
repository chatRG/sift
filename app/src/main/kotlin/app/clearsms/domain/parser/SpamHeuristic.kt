package app.clearsms.domain.parser

import app.clearsms.domain.categorizer.DltHeaderParser

/**
 * Heuristic detector for bulk marketing SMS - the middle ground between
 * "sort this as Promotional" and "block this sender".
 *
 * The shape is binary and deliberately conservative, like [ScamDetector]:
 * a false positive removes a real message from the inbox, which costs far
 * more than a false negative (one notification). It needs solicitation
 * evidence AND a way to act on it, it never fires for contacts or for a
 * header carrying a transactional/service/government DLT class, and it
 * never fires on a body with transactional artifacts.
 *
 * Everything that is KNOWLEDGE - user rules, bundled rules, the sender
 * directory, the OTP / transaction / reminder parsers - runs before this in
 * [app.clearsms.domain.categorizer.MessageCategorizer], so a heuristic can
 * never outrank a fact. The categorizer's invariants then get the final say.
 */
class SpamHeuristic(
    private val dlt: (String) -> app.clearsms.domain.categorizer.DltHeader? = DltHeaderParser::parse,
) {
    fun isSpam(
        sender: String,
        body: String,
        isContact: Boolean,
    ): Boolean {
        if (isContact) return false

        // A transactional / service / government header is never bulk
        // marketing, whatever the body says. `-P` alone is NOT proof of
        // spam: aggregators reuse headers and banks mis-register, so the
        // content still has to qualify.
        if (dlt(sender)?.isTransactional == true) return false

        if (!BULK_MARKER.containsMatchIn(body)) return false
        if (!COMMERCIAL_INTENT.containsMatchIn(body)) return false
        if (!REACHABILITY.containsMatchIn(body)) return false

        // A real statement, debit alert, bill or payment confirmation always
        // wins over a lexicon. The categorizer's own invariant would lift it
        // back out of Spam anyway; refusing here keeps the decision honest.
        if (GuardLibrary.matches(GuardId.FINANCIAL_EVIDENCE, body)) return false

        return true
    }

    private companion object {
        /** Bulk-mail / opt-out vocabulary - a mass send, not a conversation. */
        val BULK_MARKER =
            Regex(
                "(?i)\\b(?:unsubscribe|opt[\\s-]?out|stop\\s+to|t&c|terms\\s+and\\s+conditions|" +
                    "limited\\s+(?:period|time|offer)|offer\\s+ends|last\\s+chance|apply\\s+now|" +
                    "avail\\s+now|book\\s+now|click\\s+here|missed\\s+call)\\b",
            )

        /** Something being sold or solicited. */
        val COMMERCIAL_INTENT =
            Regex(
                "(?i)\\b(?:loan|pre[\\s-]?approved|credit\\s+card|insurance|policy|mutual\\s+fund|" +
                    "demat|trading|investment|property|plot|flat|admission|coaching|discount|cashback|" +
                    "voucher|coupon|recharge|upgrade|subscription)\\b",
            )

        /** A way to act on the pitch - a link, a shortcode, or a phone number. */
        val REACHABILITY =
            Regex("(?i)(?:https?://|www\\.|\\b\\d{5,6}\\b|\\b1800\\b|\\bcall\\b)")
    }
}
