package ap.andruav_ap.denative.providers.phone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AN-0128: the SMS surface of the "phone" namespace.
 *
 * keywords: normalized keyword list ({@code SmsKeywords.parse} output)
 * advertised as class-"sms" resources {@code sms_<kw>} under the
 * {@code *.received} fact pattern. contacts: contact name (lowercase)
 * -&gt; number, built from the "name=number" SMS allow-list; it is the
 * only address book {@code phone.sms_send} may use - it accepts a name,
 * never a number. sender: the send seam ({@code SMS.sendSMS} in the
 * app, a recorder in tests). send_limit/send_window_ms rate-limit
 * sms_send so a mission loop cannot send without limit.
 */
public class PhoneSmsConfig {

    /** The send seam: phone number + message text. */
    public interface SmsSender {
        void send(String number, String text);
    }

    public List<String> keywords = new ArrayList<>();
    public Map<String, String> contacts = new HashMap<>();
    public SmsSender sender = null;
    public int send_limit = 3;
    public long send_window_ms = 60_000L;
}
