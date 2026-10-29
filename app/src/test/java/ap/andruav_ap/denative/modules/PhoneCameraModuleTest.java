package ap.andruav_ap.denative.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The camera module registration: C/V features and the de_camera-format camera list. */
public class PhoneCameraModuleTest {

    private boolean mUsable;
    private JSONArray mList;
    private final List<String> mFeatures = new ArrayList<>();
    private final List<JSONArray> mLists = new ArrayList<>();
    private PhoneCameraModule mCamera;

    private static JSONObject entry(String id) throws Exception {
        return new JSONObject().put("id", id).put("ln", "camera 0").put("r", false)
                .put("v", true).put("active", 1).put("p", 2).put("s", 0).put("a", 15);
    }

    @Before
    public void setUp() throws Exception {
        mUsable = true;
        mList = new JSONArray().put(entry("back"));
        mCamera = new PhoneCameraModule(new PhoneCameraModule.Cameras() {
            @Override
            public boolean usable() {
                return mUsable;
            }

            @Override
            public JSONArray list() {
                return mList;
            }
        }, new PhoneCameraModule.Registrar() {
            @Override
            public void addFeature(String feature) {
                mFeatures.add(feature);
            }

            @Override
            public void setCameraList(JSONArray cameras) {
                mLists.add(cameras);
            }
        });
    }

    @Test
    public void usableCameraAdvertisesFeaturesAndList() {
        assertTrue(mCamera.refresh());
        assertEquals(Arrays.asList("C", "V"), mFeatures);
        assertEquals(1, mLists.size());
        assertEquals("back", mLists.get(0).optJSONObject(0).optString("id"));
    }

    @Test
    public void unchangedRefreshSendsNothing() {
        mCamera.refresh();
        assertFalse(mCamera.refresh());
        assertEquals(2, mFeatures.size());
        assertEquals(1, mLists.size());
    }

    @Test
    public void listChangeIsResent() throws Exception {
        mCamera.refresh();
        mList = new JSONArray().put(entry("front"));
        assertTrue(mCamera.refresh());
        assertEquals(2, mLists.size());
        assertEquals("front", mLists.get(1).optJSONObject(0).optString("id"));
        assertEquals(2, mFeatures.size());
    }

    @Test
    public void noPermissionMeansNoFeaturesUntilGranted() {
        mUsable = false;
        mCamera.refresh();
        assertTrue(mFeatures.isEmpty());
        assertEquals(0, mLists.get(0).length());

        mUsable = true;
        assertTrue(mCamera.refresh());
        assertEquals(Arrays.asList("C", "V"), mFeatures);
        assertEquals(1, mLists.get(mLists.size() - 1).length());
    }

    @Test
    public void incompleteEntriesAreDropped() throws Exception {
        final JSONObject noSpec = entry("usb_0");
        noSpec.remove("s");
        mList = new JSONArray().put(entry("back")).put(noSpec);
        mCamera.refresh();
        assertEquals(1, mLists.get(0).length());
    }
}
