package ap.andruav_ap.activities.phone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/***
 * AN-0138 (TASK-117): the SMS tab entry rules - case is converted and
 * accepted, spaces and bad characters are refused, reserved keywords and
 * duplicates are refused, and only full international numbers pass.
 */
public class SmsEntryValidatorTest {

    private static final List<String> CONTACTS =
            Arrays.asList("me=+966501234567", "+201001110005");
    private static final List<String> KEYWORDS =
            Arrays.asList("go", "abort");

    @Test
    public void nameConvertsToLowercaseWithoutError() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.contactName("PILOT2", CONTACTS);
        assertTrue(r.ok);
        assertEquals("pilot2", r.value);
        assertEquals(SmsEntryValidator.Error.NONE, r.error);
    }

    @Test
    public void nameWithSpaceIsRefused() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.contactName("pilot 2", CONTACTS);
        assertFalse(r.ok);
        assertEquals(SmsEntryValidator.Error.INVALID_CHARS, r.error);
    }

    @Test
    public void nameDuplicateIsRefusedCaseInsensitive() {
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.contactName("ME", CONTACTS).error);
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.contactName("me", CONTACTS).error);
    }

    @Test
    public void keywordReservedNamesAreRefused() {
        for (final String reserved : new String[]{
                "rtl", "RTL", "lnd", "land", "loc", "hlp", "auto", "goto"}) {
            final SmsEntryValidator.Result r =
                    SmsEntryValidator.keyword(reserved, KEYWORDS);
            assertFalse(reserved + " must be refused", r.ok);
            assertEquals(SmsEntryValidator.Error.RESERVED, r.error);
        }
    }

    @Test
    public void keywordDuplicateIsRefused() {
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.keyword("GO", KEYWORDS).error);
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.keyword("abort", KEYWORDS).error);
    }

    @Test
    public void keywordConvertsToLowercaseWithoutError() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.keyword("HOLD", KEYWORDS);
        assertTrue(r.ok);
        assertEquals("hold", r.value);
    }

    @Test
    public void keywordTooLongIsRefused() {
        final StringBuilder longKeyword = new StringBuilder();
        for (int i = 0; i < 29; i++) longKeyword.append('k');
        assertEquals(SmsEntryValidator.Error.TOO_LONG,
                SmsEntryValidator.keyword(longKeyword.toString(), KEYWORDS).error);
    }

    @Test
    public void numberTooShortIsRefused() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.phoneNumber("+966 55 123", CONTACTS);
        assertFalse(r.ok);
        assertEquals(SmsEntryValidator.Error.NUMBER_LENGTH, r.error);
    }

    @Test
    public void numberWithoutPlusIsRefused() {
        assertEquals(SmsEntryValidator.Error.NUMBER_FORMAT,
                SmsEntryValidator.phoneNumber("966551234567", CONTACTS).error);
    }

    @Test
    public void numberDuplicateIsRefused() {
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.phoneNumber("+966 501234567", CONTACTS).error);
        assertEquals(SmsEntryValidator.Error.DUPLICATE,
                SmsEntryValidator.phoneNumber("+201001110005", CONTACTS).error);
    }

    @Test
    public void numberIsNormalizedAndAccepted() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.phoneNumber("+966 55-1234567", CONTACTS);
        assertTrue(r.ok);
        assertEquals("+966551234567", r.value);
    }

    @Test
    public void emptyFieldsAreRefused() {
        assertEquals(SmsEntryValidator.Error.EMPTY,
                SmsEntryValidator.contactName("  ", CONTACTS).error);
        assertEquals(SmsEntryValidator.Error.EMPTY,
                SmsEntryValidator.keyword("", KEYWORDS).error);
        assertEquals(SmsEntryValidator.Error.EMPTY,
                SmsEntryValidator.phoneNumber(null, CONTACTS).error);
    }

    @Test
    public void numberWithoutListSkipsDuplicateCheck() {
        final SmsEntryValidator.Result r =
                SmsEntryValidator.phoneNumber("+966501234567",
                        Collections.emptyList());
        assertTrue(r.ok);
    }
}
