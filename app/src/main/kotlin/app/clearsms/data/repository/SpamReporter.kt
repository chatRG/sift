package app.clearsms.data.repository

import app.clearsms.domain.rules.SenderRule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of filing a sender as spam, for the confirmation snackbar.
 *
 * [mixedTraffic] means the same normalized sender also speaks as a
 * transactional header, so some of its messages will stay in the inbox
 * (the categorizer's invariants lift OTPs, transactions and bills out of
 * Spam). The report still applies - the copy just tells the truth.
 */
data class SpamReport(
    val senderCore: String,
    val messages: Int,
    val mixedTraffic: Boolean,
)

/**
 * The spam actions the UI needs, as a seam: the inbox view model depends on
 * this rather than the concrete reporter, so tests can supply a fake without
 * a MessageDao.
 */
interface SpamReporting {
    suspend fun report(sender: String): SpamReport?

    suspend fun notSpam(sender: String): Int

    suspend fun isFiledAsSpam(sender: String): Boolean
}

/**
 * THE single entry point for "report spam" / "not spam", mirroring
 * [SenderBlocker].
 *
 * Reporting is deliberately NOT blocking: it creates an ordinary
 * sender-scoped user rule (priority 1001) and re-sorts the sender's existing
 * messages, so the messages are KEPT under the Spam pill and stay
 * recoverable. Blocking keeps its own destructive, separately-confirmed
 * semantics.
 *
 * Because the rule goes through [app.clearsms.domain.categorizer.MessageCategorizer],
 * [app.clearsms.domain.categorizer.MessageCategorizer.enforceInvariants]
 * still applies: an extractable OTP, a parsed transaction, a payment
 * obligation or financial evidence is lifted back out of Spam even under an
 * explicit user spam rule. A sorting preference must never hide a
 * verification code or a money movement.
 *
 * Rule ids are deterministic (`user_spam_<core>`), so "is this sender filed
 * as spam?" is a primary-key lookup and the list is enumerable by prefix.
 */
@Singleton
class SpamReporter
    @Inject
    constructor(
        private val ruleRepository: RuleRepository,
        private val messageRepository: MessageRepository,
        private val senderTraffic: SenderTraffic,
    ) : SpamReporting {
        /** Files [sender] as spam. Returns null when the sender is unusable. */
        override suspend fun report(sender: String): SpamReport? {
            val core = SenderRule.senderCore(sender)
            if (core.isEmpty()) return null

            // Never leave two equal-priority sender rules for the same core:
            // the engine's tie-break would decide which one wins.
            removeSenderOnlyRules(sender)

            ruleRepository.addUserRule(SenderRule.definition(sender, CATEGORY_SPAM, id = spamRuleId(core)))
            val messages = messageRepository.recategorizeSenderCore(core)
            return SpamReport(
                senderCore = core,
                messages = messages,
                mixedTraffic = senderTraffic.hasMixedDltClasses(core),
            )
        }

        /**
         * Reverses [report] (or a heuristic filing): removes the spam rule and
         * re-sorts. A permanent `promotional` guard rule takes its place, so
         * the heuristic cannot immediately re-file the sender - and because
         * the categorizer's invariants treat Promotional exactly like Spam,
         * an OTP, a transaction, a bill or financial evidence still lifts
         * straight back out to Important. ("Unknown" would NOT get that
         * protection: the invariants only rescue the two junk categories.)
         */
        override suspend fun notSpam(sender: String): Int {
            val core = SenderRule.senderCore(sender)
            if (core.isEmpty()) return 0

            val spamId = spamRuleId(core)
            if (ruleRepository.userRuleDefinitions().any { it.id == spamId }) {
                ruleRepository.deleteRule(spamId)
            }
            ruleRepository.addUserRule(
                SenderRule.definition(sender, CATEGORY_PROMOTIONAL, id = notSpamRuleId(core)),
            )
            return messageRepository.recategorizeSenderCore(core)
        }

        /** Whether [sender] currently has an explicit spam rule. */
        override suspend fun isFiledAsSpam(sender: String): Boolean {
            val core = SenderRule.senderCore(sender)
            if (core.isEmpty()) return false
            val spamId = spamRuleId(core)
            return ruleRepository.userRuleDefinitions().any { it.id == spamId }
        }

        fun spamRuleId(senderCore: String): String = RULE_ID_PREFIX + senderCore

        private suspend fun removeSenderOnlyRules(sender: String) {
            // Match on the generated literal pattern, not on the display name:
            // the name is prose ("Always spam: HDFCBK") and must never be a
            // parsing contract.
            val pattern = SenderRule.pattern(sender)
            ruleRepository
                .userRuleDefinitions()
                .filter { SenderRule.isSenderOnly(it) && it.match.senderPattern == pattern }
                .forEach { ruleRepository.deleteRule(it.id) }
        }

        private companion object {
            /** Lower-case [app.clearsms.domain.model.Category.SPAM] name. */
            const val CATEGORY_SPAM = "spam"
            const val CATEGORY_PROMOTIONAL = "promotional"
            const val RULE_ID_PREFIX = "user_spam_"

            fun notSpamRuleId(senderCore: String): String = "user_notspam_" + senderCore
        }
    }
