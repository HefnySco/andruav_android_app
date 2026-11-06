package ap.andruav_ap.activities.phone;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ap.andruav_ap.communication.telemetry.SmsKeywords;

/***
 * AN-0138 (TASK-117): entry validation for the SMS tab editors. Pure Java -
 * no Android types - so the rules run under testDebugUnitTest. The phone
 * sheets map {@link Error} codes to strings.
 *
 * Case is never an error: names and keywords are lowercased and accepted.
 * Refused: spaces or characters outside [a-z0-9_], reserved command names,
 * duplicates, and numbers that are not in full international form.
 */
public final class SmsEntryValidator {

    public enum Error {
        NONE,
        EMPTY,
        INVALID_CHARS,
        TOO_LONG,
        RESERVED,
        DUPLICATE,
        NUMBER_FORMAT,
        NUMBER_LENGTH
    }

    public static final class Result {
        public final boolean ok;
        /** normalized value: lowercased name/keyword, "+digits" number. */
        public final String value;
        public final Error error;

        private Result(final boolean ok, final String value, final Error error) {
            this.ok = ok;
            this.value = value;
            this.error = error;
        }

        static Result ok(final String value) {
            return new Result(true, value, Error.NONE);
        }

        static Result fail(final Error error) {
            return new Result(false, "", error);
        }
    }

    /** Same reserved words SmsKeywords drops - a keyword may never shadow a
     * parser command. Kept in sync with SmsKeywords.RESERVED (private). */
    private static final String[] RESERVED_KEYWORDS = {
            "hlp", "rtl", "lnd", "land", "loc", "auto", "goto" };

    /** sms_<kw> must fit the 32-char cap-name limit (SmsKeywords). */
    private static final int KEYWORD_MAX_LEN = 28;

    private static final Pattern TOKEN_RE = Pattern.compile("^[a-z0-9_]+$");

    /** Full international form: "+" + digits. Length bounds keep typos and
     * local forms out while accepting any real E.164 number. */
    private static final int NUMBER_MIN_DIGITS = 10;
    private static final int NUMBER_MAX_DIGITS = 15;

    private SmsEntryValidator() {
    }

    /**
     * Contact name: lowercased, [a-z0-9_], unique against the names already
     * in the allow-list (case-insensitive). Names feed facts and
     * phone.sms_send, so a name already in use is refused.
     */
    public static Result contactName(final String raw, final List<String> existingEntries) {
        final String name = raw != null ? raw.trim().toLowerCase(Locale.US) : "";
        if (name.isEmpty()) return Result.fail(Error.EMPTY);
        if (!TOKEN_RE.matcher(name).matches()) return Result.fail(Error.INVALID_CHARS);
        if (existingEntries != null) {
            for (final String entry : existingEntries) {
                if (name.equals(SmsKeywords.entryName(entry).toLowerCase(Locale.US))) {
                    return Result.fail(Error.DUPLICATE);
                }
            }
        }
        return Result.ok(name);
    }

    /**
     * Event keyword: lowercased, [a-z0-9_], within the cap-name budget, not a
     * reserved command name, not already in the list.
     */
    public static Result keyword(final String raw, final List<String> existingKeywords) {
        final String kw = raw != null ? raw.trim().toLowerCase(Locale.US) : "";
        if (kw.isEmpty()) return Result.fail(Error.EMPTY);
        if (!TOKEN_RE.matcher(kw).matches()) return Result.fail(Error.INVALID_CHARS);
        if (kw.length() > KEYWORD_MAX_LEN) return Result.fail(Error.TOO_LONG);
        for (final String reserved : RESERVED_KEYWORDS) {
            if (reserved.equals(kw)) return Result.fail(Error.RESERVED);
        }
        if (existingKeywords != null && existingKeywords.contains(kw)) {
            return Result.fail(Error.DUPLICATE);
        }
        return Result.ok(kw);
    }

    /**
     * Phone number in full international form: "+", then digits only.
     * Internal spaces and dashes are stripped before the check, so
     * "+966 55-123" validates as "+96655123" (and then fails on length).
     */
    public static Result phoneNumber(final String raw) {
        return phoneNumber(raw, null);
    }

    /** As above, plus a duplicate check against the allow-list numbers. */
    public static Result phoneNumber(final String raw, final List<String> existingEntries) {
        if (raw == null) return Result.fail(Error.EMPTY);
        final String stripped = raw.trim().replace(" ", "").replace("-", "");
        if (stripped.isEmpty()) return Result.fail(Error.EMPTY);
        if (stripped.charAt(0) != '+') return Result.fail(Error.NUMBER_FORMAT);
        final String digits = stripped.substring(1);
        for (int i = 0; i < digits.length(); i++) {
            final char c = digits.charAt(i);
            if (c < '0' || c > '9') return Result.fail(Error.NUMBER_FORMAT);
        }
        if (digits.length() < NUMBER_MIN_DIGITS
                || digits.length() > NUMBER_MAX_DIGITS) {
            return Result.fail(Error.NUMBER_LENGTH);
        }
        final String normalized = "+" + digits;
        if (existingEntries != null) {
            for (final String entry : existingEntries) {
                final String existing = SmsKeywords.entryNumber(entry)
                        .replaceAll("[^0-9]", "");
                if (digits.equals(existing)) {
                    return Result.fail(Error.DUPLICATE);
                }
            }
        }
        return Result.ok(normalized);
    }
}
