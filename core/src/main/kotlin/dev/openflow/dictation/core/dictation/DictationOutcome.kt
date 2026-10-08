package dev.openflow.dictation.core.dictation

/**
 * How a Dictation's text reached the user: the payload of
 * [DictationState.Done].
 *
 * ADR-0002 names this `outcome` without defining it, so the values come from
 * ADR-0003's insertion layering, which is the only thing that decides whether
 * the text arrived or only reached the clipboard. Insertion failure is *not*
 * here — that is [DictationState.ErrorRecoverable], because a Dictation whose
 * text was never delivered has not been done with.
 */
enum class DictationOutcome {

    /**
     * The text reached the target field, by `ACTION_SET_TEXT` or
     * `ACTION_PASTE`. Cursor placement after a paste is ADR-0003's documented
     * limitation, which is why this value is about arrival and not about where
     * the cursor ended up.
     */
    INSERTED,

    /**
     * The target could not be written, so the text is in the clipboard and the
     * user was prompted to paste it. The transcript is preserved, which is why
     * a Dictation can reach this state without having failed.
     */
    COPIED,

    /**
     * The copy-and-prompt fallback was offered and the user declined. Still a
     * [DictationState.Done]: nothing failed and nothing was lost, the text just
     * stayed in History. A separate value because "the user decided not to
     * paste" and "the text is waiting in the clipboard" are different facts to
     * show in History and to count in the router's decision log.
     */
    DECLINED,
}