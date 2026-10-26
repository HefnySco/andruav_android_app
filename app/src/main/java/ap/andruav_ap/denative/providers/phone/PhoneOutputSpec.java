package ap.andruav_ap.denative.providers.phone;

/**
 * Configuration of one named phone output resource (one entry of the
 * {@code phone_outputs[]} config: {@code {name, kind: screen|torch|vibrate,
 * pulse_ms?, events?, strobe_hz?, color?}} — TO-BE §5.2).
 * Pure data, kept free of Android and org.json so the choke-point logic
 * stays JVM-testable.
 */
public final class PhoneOutputSpec {

    /** Screen strobe — the full-screen {@code FlashActivity}. */
    public static final String KIND_SCREEN = "screen";
    /** Camera torch — {@code PeerConnectionManager.setFlash} while WebRTC owns
     *  the camera, {@code CameraManager.setTorchMode} otherwise. */
    public static final String KIND_TORCH = "torch";
    /** {@code Vibrator} pattern output. */
    public static final String KIND_VIBRATE = "vibrate";

    /** Resource class advertised in {@code resources} — every phone output is class "out". */
    public static final String CLASS_OUT = "out";

    /** Addressable key ({@code <ns>.<name>.<verb>}). */
    public final String name;
    /** Which physical output backs this resource: {@link #KIND_SCREEN} / {@link #KIND_TORCH} / {@link #KIND_VIBRATE}. */
    public final String kind;
    /** Default length for a {@code pulse} command carrying no {@code ms}. */
    public int pulseMs = 1000;
    /** Gates the whole event surface (commands AND facts), like gpio's {@code events} flag. */
    public boolean events = true;
    /** Strobe/blink rate for screen and torch (torch: 0 = steady). */
    public double strobeHz = 4.0;
    /** Strobe color (0xRRGGBB); screen only. */
    public int color = 0xFFFFFF;

    public PhoneOutputSpec(final String name, final String kind) {
        this.name = name;
        this.kind = kind;
    }
}
