package app.clearsms.domain.categorizer

import app.clearsms.data.repository.SenderNormalizer

/**
 * The TRAI header class carried by the last character of an A2P sender id:
 * `-P` promotional, `-T` transactional, `-S` service, `-G` government.
 *
 * Indian aggregators append the class to the registered header
 * (`VM-HDFCBK-P`), and the app has always stripped it for thread identity
 * ([SenderNormalizer]) without ever reading it. This is the one place that
 * gives the suffix a meaning.
 */
enum class DltClass(
    val suffix: Char,
) {
    PROMOTIONAL('P'),
    TRANSACTIONAL('T'),
    SERVICE('S'),
    GOVERNMENT('G'),
}

/**
 * A parsed alphanumeric A2P sender id.
 *
 * [header] is exactly what [SenderNormalizer.normalize] returns for the same
 * sender - the two must never disagree, so a test pins the agreement over a
 * sample corpus. [dltClass] is null when the sender carries no recognised
 * suffix; that is common and means "unknown", never "promotional".
 */
data class DltHeader(
    val routePrefix: String?,
    val header: String,
    val dltClass: DltClass?,
) {
    /** True only for the classes that must never be heuristically filed as spam. */
    val isTransactional: Boolean
        get() = dltClass != null && dltClass != DltClass.PROMOTIONAL
}

/**
 * Parses the TRAI route prefix and DLT class suffix off an alphanumeric
 * sender id. Pure Kotlin, no Android dependencies.
 *
 * Returns null for phone numbers, short codes and anything without a letter -
 * those are P2P traffic where the suffix convention does not apply.
 */
object DltHeaderParser {
    private val ROUTE_PREFIX = Regex("^[A-Z]{2}-")
    private val HEADER = Regex("^[A-Z0-9]{2,}$")

    fun parse(sender: String): DltHeader? {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return null
        if (SenderNormalizer.isPhoneNumber(trimmed)) return null

        val upper = trimmed.uppercase()
        // A short code such as 56767 is alphanumeric-sender-shaped but has no
        // letter and no class; it must not produce a header.
        if (upper.none { it in 'A'..'Z' }) return null

        val prefixMatch = ROUTE_PREFIX.find(upper)
        val routePrefix = prefixMatch?.value?.removeSuffix("-")
        val withoutPrefix = if (prefixMatch == null) upper else upper.substring(prefixMatch.range.last + 1)

        val suffixChar = withoutPrefix.lastOrNull()
        val dltClass = DltClass.entries.firstOrNull { it.suffix == suffixChar }
        val header = if (dltClass != null && withoutPrefix.length > 2) withoutPrefix.dropLast(2) else withoutPrefix

        if (!HEADER.matches(header)) return null
        return DltHeader(routePrefix = routePrefix, header = header, dltClass = dltClass)
    }

    /** The class suffix of [sender], or null when it carries none. */
    fun dltClass(sender: String): DltClass? = parse(sender)?.dltClass

    /** Whether [sender] is a recognised `-T` / `-S` / `-G` header. */
    fun isTransactional(sender: String): Boolean = parse(sender)?.isTransactional == true
}
