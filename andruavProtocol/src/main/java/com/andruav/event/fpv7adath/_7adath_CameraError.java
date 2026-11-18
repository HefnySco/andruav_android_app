package com.andruav.event.fpv7adath;

/**
 * The camera could not be opened or failed while running - carries the
 * reason text so a UI can show it instead of a black preview.
 */
public class _7adath_CameraError {

    /** Reason text from the capturer, e.g. "Camera 0 is in use by another app.". */
    public final String description;

    public _7adath_CameraError(final String description) {
        this.description = description;
    }
}
