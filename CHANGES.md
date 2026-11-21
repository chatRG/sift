# Changes from Clear SMS

Sift is a modified version of Clear SMS, originally published at
https://github.com/itsluminous/ClearSMS under the Apache License 2.0. This file
records the modifications, as required by section 4(b) of that licence. It is
not a complete commit log; see the repository history for that.

## Added

- **Spam filter.** A dedicated Spam folder, a "Report spam" / "Not spam"
  action, automatic quarantine (spam leaves the All view and never notifies),
  and an "Empty spam" action. Reporting is a reversible sender rule, not a
  delete; blocking keeps its own destructive, separately-confirmed path.
  Heuristics that file bulk marketing are vetoed by transactional DLT headers
  (`-T` / `-S` / `-G`), contacts and financial evidence, and the categorizer's
  invariants still lift OTPs, transactions and bills out of Spam.
- **DLT header parsing** (`-P` / `-T` / `-S` / `-G`), used by the spam
  heuristics and by the mixed-traffic warning.

## Fixed

- **OTP auto-delete now purges the phone's SMS store.** The scheduled
  `OtpAutoDeleteWorker` deleted rows straight through `MessageDao`, which
  removed them from the app's own database but left them in Android's Telephony
  provider. After a reinstall, or once another app held the default-SMS role,
  the "deleted" OTPs were still there. The worker now goes through
  `MessageRepository.deleteOtpOlderThan`, the same batched path that also syncs
  the provider delete.
- **Blocking a sender can no longer destroy a conversation.** With the recycle
  bin off, "Block" used to hard-delete every message in the thread, in the app
  and in the system SMS store, with no undo. Blocking now always moves the
  conversation to the recycle bin, and bins every live thread the sender owns
  rather than one arbitrary thread id (upstream #159).
- **Keyword blocking now applies to MMS.** The blocked-keyword list was checked
  on SMS only; an MMS whose body matched a blocked keyword still notified. Both
  the live download path and the history import now apply the same check as SMS
  (upstream #133).
- **SBI cheque-clearing debits are parsed.** `Chq 12345 for INR 500.00 recd in
  clearing debited from AC ... Avl bal INR ...` matched no rule, so the debit
  was missing from Finance and the running balance stayed stale. New bundled
  rule `sbi-cheque-clear-debit-01` (upstream #155).
- **Sentence capitalisation in the composer**, and a phone keypad plus "Next"
  on the recipient field (upstream #171).

## Rebranded

- The application is named **Sift**, with a new `applicationId`
  (`app.sift.sms`) so it installs beside Clear SMS. Every user-facing string
  resource in every locale uses the new name; the Kotlin package and database
  identifiers are unchanged, deliberately, so the app's data model and
  migrations stay identical to upstream.

## Removed

- The original author's personal payment endpoints (PayPal handle and UPI VPA)
  and their Settings rows.
- The "share rules with the developer" email address; sharing rules now opens
  the ordinary Android share sheet.
- The upstream F-Droid publishing recipe, store metadata and the original
  app's changelog history (`fastlane/`).
- The generated translation-coverage table, its enforcing test
  (`TranslationStatusTest`) and `scripts/translation_status.py`.
- Store badges, links and screenshots from `README.md`, which described the
  upstream distribution rather than this fork.

The upstream copyright and attribution notices, the `LICENSE` file, the
`NOTICE` file and all third-party MIT notices are retained unchanged.
