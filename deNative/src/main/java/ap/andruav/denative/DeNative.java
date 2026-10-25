package ap.andruav.denative;

public class DeNative {

    static {
        System.loadLibrary("de_host");
    }

    public static native String version();
}
