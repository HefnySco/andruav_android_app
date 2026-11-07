package ap.andruav_ap.communication.telemetry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/***
 * AN-0137 (TASK-116): the side-effect-free verdict of the SMS command gate.
 * {@link AndruavSMSClientParser#evaluate} produces one of these per message;
 * executeCommand then replays the verdict against the real world. A dry run
 * stops at the verdict - no SMS, no GCS notice, no PIN count, no command.
 *
 * The gates mirror the live order exactly: receiver, lockout, sender,
 * format, pin, age, command. A gate after a FAIL is reported SKIP.
 */
public final class SmsGateVerdict {

    public enum Status { PASS, FAIL, SKIP }

    /** One gate's outcome - name is stable for the UI, detail is a short human line. */
    public static final class Gate {
        public final String name;
        public final Status status;
        public final String detail;

        public Gate(final String name, final Status status, final String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    public final List<Gate> gates;
    /** Every gate passed - the action would run for real. */
    public final boolean allowed;
    /** The refusal detail when allowed=false, "" otherwise. */
    public final String refuseReason;
    /** The GCS line the live path would emit on refusal, or null when it emits none. */
    public final String gcsRefuseText;
    /** The "SMS accepted" info line the live path emits before the refusal when
     *  the identity gates already passed (command-gate failures), or null. */
    public final String gcsAcceptText;
    /** The matched allow-list contact name ("" for a bare-number entry or no match). */
    public final String contact;
    /** Display form of the sender for notices: "'name'" or "an allow-listed contact". */
    public final String who;
    /** The resolved command token (uppercase), "" when resolution failed. */
    public final String command;
    /** The matched keyword (lowercase) or "". */
    public final String keyword;
    /** The fact that would fire for a keyword ("phone.sms_<kw>") or "". */
    public final String fact;
    /** Message age in seconds the fact payload would carry. */
    public final long age_s;
    /** true when the PIN gate failed - the actor must count one PIN failure. */
    public final boolean pinFailure;
    /** The AUTO step when the command is "AUTO N", else -1. */
    public final int autoStep;
    /** One-line human summary of what would follow ("RTL command", "fact phone.sms_go"). */
    public final String actionText;

    private SmsGateVerdict(final List<Gate> gates,
                           final boolean allowed,
                           final String refuseReason,
                           final String gcsRefuseText,
                           final String gcsAcceptText,
                           final String contact,
                           final String who,
                           final String command,
                           final String keyword,
                           final long age_s,
                           final boolean pinFailure,
                           final int autoStep,
                           final String actionText) {
        this.gates = Collections.unmodifiableList(new ArrayList<>(gates));
        this.allowed = allowed;
        this.refuseReason = refuseReason;
        this.gcsRefuseText = gcsRefuseText;
        this.gcsAcceptText = gcsAcceptText;
        this.contact = contact;
        this.who = who;
        this.command = command;
        this.keyword = keyword;
        this.fact = keyword.isEmpty() ? "" : "phone.sms_" + keyword;
        this.age_s = age_s;
        this.pinFailure = pinFailure;
        this.autoStep = autoStep;
        this.actionText = actionText;
    }

    /** Mutable builder used only inside evaluate() - verdicts themselves are immutable. */
    static final class Builder {
        final List<Gate> gates = new ArrayList<>();
        boolean allowed = true;
        String refuseReason = "";
        String gcsRefuseText = null;
        String gcsAcceptText = null;
        String contact = "";
        String who = "";
        String command = "";
        String keyword = "";
        long age_s = 0;
        boolean pinFailure = false;
        int autoStep = -1;
        String actionText = "";

        private boolean failed;

        void pass(final String name, final String detail) {
            gates.add(new Gate(name, failed ? Status.SKIP : Status.PASS, detail));
        }

        void fail(final String name, final String reason) {
            gates.add(new Gate(name, Status.FAIL, reason));
            failed = true;
            allowed = false;
            refuseReason = reason;
        }

        SmsGateVerdict build() {
            /* the sheet lists every gate - after a FAIL the ones that never
             * ran are reported SKIP so the row count is always the full set. */
            if (failed) {
                for (final String name : GATE_ORDER) {
                    boolean present = false;
                    for (final Gate g : gates) {
                        if (g.name.equals(name)) { present = true; break; }
                    }
                    if (!present) {
                        gates.add(new Gate(name, Status.SKIP, "not reached"));
                    }
                }
            }
            return new SmsGateVerdict(gates, allowed, refuseReason, gcsRefuseText, gcsAcceptText,
                    contact, who, command, keyword, age_s, pinFailure, autoStep, actionText);
        }
    }

    /** The live gate order - the verdict always reports all of them. */
    static final String[] GATE_ORDER = {
            "receiver", "lockout", "sender", "format", "pin", "age", "command" };
}
