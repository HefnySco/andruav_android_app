package ap.andruavmiddlelibrary.webrtc.camera;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class ZoomMathTest {

    // 4000x3000 sensor active array at origin, zoomMax 10.
    private static final int[] FULL = {0, 0, 4000, 3000};

    @Test
    public void zoomZeroCoversWholeActiveArray() {
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, 0.0f, 10.0f));
    }

    @Test
    public void zoomOneUsesMaxDigitalZoom() {
        // factor 10 -> 400x300 crop centered on (2000,1500).
        assertArrayEquals(new int[] {1800, 1350, 2200, 1650},
                ZoomMath.cropRegion(0, 0, 4000, 3000, 1.0f, 10.0f));
    }

    @Test
    public void zoomHalfIsCentered() {
        // factor 1 + 0.5*(10-1) = 5.5 -> half-extents 363x272.
        assertArrayEquals(new int[] {1637, 1228, 2363, 1772},
                ZoomMath.cropRegion(0, 0, 4000, 3000, 0.5f, 10.0f));
    }

    @Test
    public void outOfRangeValuesClamp() {
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, -0.5f, 10.0f));
        assertArrayEquals(ZoomMath.cropRegion(0, 0, 4000, 3000, 1.0f, 10.0f),
                ZoomMath.cropRegion(0, 0, 4000, 3000, 2.5f, 10.0f));
    }

    @Test
    public void noZoomSupportAlwaysFullArray() {
        // zoomMax 1.0 = no digital zoom: every fraction crops to the full array.
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, 0.5f, 1.0f));
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, 1.0f, 1.0f));
    }

    @Test
    public void degenerateZoomMaxCannotExceedActiveArray() {
        // zoomMax below 1 is nonsense - must never produce a factor < 1.
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, 0.0f, 0.5f));
        assertArrayEquals(FULL,
                ZoomMath.cropRegion(0, 0, 4000, 3000, 1.0f, 0.5f));
    }

    @Test
    public void nonZeroOriginActiveArrayStaysCentered() {
        // Active arrays need not start at (0,0): a 4000x3000 array at (100,200)
        // centers on (2100,1700); factor 10 -> 400x300 crop around it.
        assertArrayEquals(new int[] {1900, 1550, 2300, 1850},
                ZoomMath.cropRegion(100, 200, 4000, 3000, 1.0f, 10.0f));
        assertArrayEquals(new int[] {100, 200, 4100, 3200},
                ZoomMath.cropRegion(100, 200, 4000, 3000, 0.0f, 10.0f));
    }
}
