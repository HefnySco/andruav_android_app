package ap.andruav_ap.communication.telemetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * AN-0128: shared rules for the SMS keyword surface
 * ({@code phone.sms_<keyword>.received} facts).
 *
 * Keywords come from the phone settings as a comma-separated list
 * ("go, abort"). A keyword becomes the resource key
 * {@code sms_<keyword>}, so it is normalized to lowercase and must
 * produce a valid de.cap name (a-z, 0-9, underscore, and the full key
 * stays within the 32-char cap-name limit). Keywords that name a
 * parser command (RTL, LND, LOC, HLP, AUTO...) are dropped: a reserved
 * word always runs its command and never fires a fact.
 */
public final class SmsKeywords {

    /** Commands owned by the SMS command parser (its switch + the
     *  AUTO-with-step form). A keyword may never shadow one of these. */
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            "hlp", "rtl", "lnd", "land", "loc", "auto", "goto"));

    /** sms_<kw> must fit the cap-name limit of 32 chars. */
    private static final int KEYWORD_MAX_LEN = 28;

    private static final Pattern KEYWORD_RE = Pattern.compile("^[a-z0-9_]+$");

    private SmsKeywords() {
    }

    /**
     * "GO, abort ,GO,rtl" -&gt; ["go","abort"]: lowercased, trimmed,
     * deduped, invalid-name and reserved entries dropped.
     */
    public static List<String> parse(final String csv) {
        final Set<String> out = new LinkedHashSet<>();
        if (csv == null) return new ArrayList<>(out);
        for (final String raw : csv.split(",")) {
            final String kw = raw.trim().toLowerCase();
            if (kw.isEmpty() || kw.length() > KEYWORD_MAX_LEN
                    || !KEYWORD_RE.matcher(kw).matches()
                    || RESERVED.contains(kw)) {
                continue;
            }
            out.add(kw);
        }
        return new ArrayList<>(out);
    }

    /** The keyword a command token resolves to (lowercase), or null. */
    public static String match(final String cmd, final List<String> keywords) {
        if (cmd == null || keywords == null) return null;
        final String kw = cmd.trim().toLowerCase();
        return keywords.contains(kw) ? kw : null;
    }

    /** The resource key for the advert and the fact name stem. */
    public static String resourceKey(final String keyword) {
        return "sms_" + keyword;
    }

    /**
     * Contact-list entries are "name=number" or a bare "number"; the
     * number is what a sender is matched against.
     */
    public static String entryNumber(final String entry) {
        final int eq = entry.indexOf('=');
        return (eq >= 0) ? entry.substring(eq + 1).trim() : entry.trim();
    }

    /** The name part of a "name=number" entry, or "" for a bare number. */
    public static String entryName(final String entry) {
        final int eq = entry.indexOf('=');
        return (eq >= 0) ? entry.substring(0, eq).trim() : "";
    }

    /**
     * name (lowercase) -&gt; number map for the phone.sms_send action;
     * built from the "name=number" allow-list entries. Bare numbers get
     * no name and can never be a send target.
     */
    public static Map<String, String> contactBook(final List<String> entries) {
        final Map<String, String> book = new HashMap<>();
        if (entries == null) return book;
        for (final String entry : entries) {
            final String name = entryName(entry);
            final String number = entryNumber(entry);
            if (!name.isEmpty() && !number.isEmpty()) {
                book.put(name.toLowerCase(), number);
            }
        }
        return book;
    }
}
