package app.clearsms.data.rules

import app.clearsms.data.db.RuleEntity
import app.clearsms.domain.parser.GuardLibrary
import kotlinx.serialization.json.Json

/**
 * Parses a user-supplied rules JSON document into user rule rows.
 *
 * Imported rules are persisted and then evaluated against EVERY incoming
 * message, so this is the trust boundary for community/user rule content:
 * documents with too many rules are rejected, and every regex pattern is run
 * through [GuardLibrary.validate] - the SAME load-time discipline the bundled
 * guard library and parser tables get: an oversized pattern, a leading or
 * trailing `.*`/`[\s\S]*` wrapper, a nested unbounded quantifier (the classic
 * `(a+)+` explosion) or a variable-length lookbehind is refused with a
 * descriptive error before anything reaches the database. A blank pattern is
 * refused too: an empty regex matches every message, so a rule carrying one
 * would swallow the whole inbox into its category.
 *
 * The engine's per-message budget cannot interrupt a single running
 * [Regex.find], so rejecting the shapes up front is what actually keeps a
 * pathological imported rule from stalling ingestion.
 */
class RuleImporter(
    private val json: Json,
) {
    /**
     * Parses [text] as a rules document and returns the rows to insert.
     *
     * @throws IllegalArgumentException when the document cannot be parsed or
     * fails validation.
     */
    fun import(text: String): List<RuleEntity> {
        val document =
            try {
                json.decodeFromString(RuleDocument.serializer(), text)
            } catch (e: Exception) {
                throw IllegalArgumentException("Not a valid rules document", e)
            }
        require(document.rules.size <= MAX_RULES_PER_IMPORT) {
            "Too many rules: ${document.rules.size} (maximum $MAX_RULES_PER_IMPORT per import)"
        }
        for (rule in document.rules) {
            validatePattern(rule.id, rule.match.senderPattern)
            validatePattern(rule.id, rule.match.bodyPattern)
        }
        return document.rules.map { it.toEntity(json, RuleSources.USER) }
    }

    private fun validatePattern(
        ruleId: String,
        pattern: String?,
    ) {
        if (pattern == null) return
        val reason = GuardLibrary.validate(pattern)
        if (reason != null) throw IllegalArgumentException("Rule '$ruleId': $reason")
    }

    companion object {
        /** Upper bound on rules per imported document. */
        const val MAX_RULES_PER_IMPORT = 200

        /** Upper bound on a single regex pattern's length; the shared validator's cap. */
        const val MAX_PATTERN_LENGTH = GuardLibrary.MAX_PATTERN_LENGTH
    }
}
