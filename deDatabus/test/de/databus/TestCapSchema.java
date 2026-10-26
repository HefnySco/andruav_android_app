package de.databus;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Port of de_databus/python/test_cap_schema.py — de.cap/1 advert
 * validator tests (TASK-PF-02 resources/event_patterns, TASK-PF-01
 * action "claim").
 */
class TestCapSchema {

    private static JSONObject pf02Advert() {
        return new JSONObject("{\n"
                + "\"schema\":\"de.cap/1\",\n"
                + "\"ns\":\"gpio\",\n"
                + "\"module\":\"droneengage_rpi_gpio\",\"ver\":\"1.4.1\",\n"
                + "\"actions\":{\n"
                + "  \"write\":{\"params\":{\"pin\":{\"type\":[\"string\",\"int\"]},\n"
                + "                       \"value\":{\"type\":\"int\",\"min\":0}}},\n"
                + "  \"read\":{\"params\":{\"pin\":{\"type\":[\"string\",\"int\"]}}}\n"
                + "},\n"
                + "\"resources\":{\"led\":\"out\",\"servo\":\"pwm\",\"btn\":\"in\"},\n"
                + "\"event_patterns\":{\n"
                + "  \"*.went_on\":{\"kind\":\"fact\",\"payload\":{\"v\":{\"type\":\"int\"},\n"
                + "                                           \"src\":{\"type\":\"string\"}}},\n"
                + "  \"*.went_off\":{\"kind\":\"fact\",\"payload\":{\"v\":{\"type\":\"int\"},\n"
                + "                                            \"src\":{\"type\":\"string\"}}},\n"
                + "  \"*.changed\":{\"kind\":\"fact\",\"res\":[\"pwm\"],\n"
                + "               \"payload\":{\"v\":{\"type\":\"int\"},\"src\":{\"type\":\"string\"}}},\n"
                + "  \"*.on\":{\"kind\":\"command\",\"res\":[\"out\",\"pwm\"]},\n"
                + "  \"*.off\":{\"kind\":\"command\",\"res\":[\"out\",\"pwm\"]},\n"
                + "  \"*.pulse\":{\"kind\":\"command\",\"res\":[\"out\"],\n"
                + "             \"payload\":{\"ms\":{\"type\":\"int\",\"optional\":true,\n"
                + "                                \"min\":1,\"max\":4000}}},\n"
                + "  \"*.set\":{\"kind\":\"command\",\"res\":[\"pwm\"],\n"
                + "           \"payload\":{\"v\":{\"type\":\"int\",\"min\":0,\"max\":1024}}}\n"
                + "},\n"
                + "\"state\":{\"led\":{\"type\":\"int\"},\"servo\":{\"type\":\"int\"},\n"
                + "         \"btn\":{\"type\":\"int\"}}\n"
                + "}");
    }

    private static JSONObject prePf02Advert() {
        return new JSONObject("{"
                + "\"schema\":\"de.cap/1\",\"ns\":\"camera\","
                + "\"module\":\"droneengage_camera\",\"ver\":\"3.2.1\","
                + "\"actions\":{\"capture\":{\"params\":{\"count\":{\"type\":\"int\"}}}},"
                + "\"events\":{\"captured\":{\"payload\":{\"file\":{\"type\":\"string\"}}}},"
                + "\"state\":{\"recording\":{\"type\":\"bool\"}}"
                + "}");
    }

    // ----- TestEventPatternsValid -----

    @Test
    void pf02_advert_validates() {
        assertEquals("", CapSchema.cap_validate_advert(pf02Advert()));
    }

    @Test
    void advert_with_only_new_keys_validates() {
        JSONObject advert = new JSONObject("{\"schema\":\"de.cap/1\",\"ns\":\"gpio\","
                + "\"module\":\"m\",\"ver\":\"1\",\"resources\":{\"led\":\"out\"},"
                + "\"event_patterns\":{\"*.went_on\":{\"kind\":\"fact\"}}}");
        assertEquals("", CapSchema.cap_validate_advert(advert));
    }

    @Test
    void pre_pf02_advert_unchanged() {
        assertEquals("", CapSchema.cap_validate_advert(prePf02Advert()));
    }

    @Test
    void resources_shape() {
        JSONObject a = pf02Advert();
        a.put("resources", new org.json.JSONArray().put("led"));
        assertNotEquals("", CapSchema.cap_validate_advert(a));

        for (Object[] bad : new Object[][]{{"Bad Key", "out"}, {"led", 7}, {"led", "Out"}}) {
            a = pf02Advert();
            a.getJSONObject("resources").put((String) bad[0], bad[1]);
            assertNotEquals("", CapSchema.cap_validate_advert(a));
        }
    }

    @Test
    void event_pattern_keys() {
        for (String bad_key : new String[]{"went_on", "*.*.*", "*.Went_on", "*." + "a".repeat(33)}) {
            JSONObject a = pf02Advert();
            a.getJSONObject("event_patterns").put(bad_key, new JSONObject());
            assertNotEquals("", CapSchema.cap_validate_advert(a), bad_key);
        }
        JSONObject a = pf02Advert();
        a.put("event_patterns", new org.json.JSONArray().put("*.went_on"));
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").put("*.x", "fact");
        assertNotEquals("", CapSchema.cap_validate_advert(a));
    }

    @Test
    void pattern_fields() {
        JSONObject a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.on").put("kind", "query");
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.on").put("kind", 3);
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.on").put("res", "out");
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.on")
                .put("res", new org.json.JSONArray().put("Out"));
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.on")
                .put("res", new org.json.JSONArray().put(3));
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.went_on").put("desc", 1);
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.getJSONObject("event_patterns").getJSONObject("*.pulse")
                .getJSONObject("payload").getJSONObject("ms").put("type", "frobnicate");
        assertNotEquals("", CapSchema.cap_validate_advert(a));
    }

    @Test
    void dotted_events_key_still_rejected() {
        // the pre-PF-02 fake stays rejected - this is why the keys had to
        // be top-level
        JSONObject a = pf02Advert();
        a.put("events", new JSONObject().put("*.went_on", new JSONObject()));
        assertNotEquals("", CapSchema.cap_validate_advert(a));
        a = pf02Advert();
        a.put("events", new JSONObject().put("led.went_on", new JSONObject()));
        assertNotEquals("", CapSchema.cap_validate_advert(a));
    }

    // ----- TestMatchEventPattern -----

    private JSONObject advert;

    @BeforeEach
    void setUp() {
        advert = pf02Advert();
    }

    private Object match(String key, String verb, String kind) {
        return CapSchema.cap_match_event_pattern(advert, key, verb, kind);
    }

    @Test
    void facts() {
        assertNotNull(match("led", "went_on", "fact"));
        assertNotNull(match("btn", "went_on", "fact"));
        // "res" class whitelist: changed is pwm-only
        assertNotNull(match("servo", "changed", "fact"));
        assertNull(match("led", "changed", "fact"));
    }

    @Test
    void commands() {
        assertNotNull(match("led", "on", "command"));
        assertNotNull(match("servo", "set", "command"));
        // an input resource takes no commands
        assertNull(match("btn", "on", "command"));
        assertNull(match("btn", "pulse", "command"));
    }

    @Test
    void kind_never_crosses() {
        assertNull(match("led", "on", "fact"));
        assertNull(match("led", "went_on", "command"));
    }

    @Test
    void unknown_verb_and_key() {
        assertNull(match("led", "went_onn", "fact"));
        assertNull(match("nosuch", "went_on", "fact"));
    }

    @Test
    void kind_defaults_to_fact() {
        advert.getJSONObject("event_patterns").put("*.flipped", new JSONObject());
        assertNotNull(match("led", "flipped", "fact"));
        assertNull(match("led", "flipped", "command"));
    }

    @Test
    void no_patterns_no_match() {
        assertNull(CapSchema.cap_match_event_pattern(prePf02Advert(), "led", "went_on", "fact"));
    }

    @Test
    void no_resources_key_unchecked() {
        advert.remove("resources");
        assertNotNull(match("anykey", "went_on", "fact"));
        assertNotNull(match("anykey", "on", "command"));
        assertNotNull(match("anykey", "set", "command"));
    }

    @Test
    void malformed_never_crashes() {
        JSONObject a = pf02Advert();
        a.put("event_patterns", 5);
        assertNull(CapSchema.cap_match_event_pattern(a, "led", "went_on", "fact"));
        a = pf02Advert();
        a.put("resources", 5);
        assertNull(CapSchema.cap_match_event_pattern(a, "led", "went_on", "fact"));
    }

    // ----- TestActionClaim -----

    @Test
    void claim_spellings_accepted() {
        JSONObject a = pf02Advert();
        a.getJSONObject("actions").getJSONObject("write").put("claim", "ns");
        a.getJSONObject("actions").getJSONObject("read").put("claim", "action");
        assertEquals("", CapSchema.cap_validate_advert(a));
    }

    @Test
    void bad_claim_rejected() {
        for (Object bad : new Object[]{"pin", 1, true, JSONObject.NULL,
                new org.json.JSONArray().put("action"), new JSONObject().put("c", "action")}) {
            JSONObject a = pf02Advert();
            a.getJSONObject("actions").getJSONObject("read").put("claim", bad);
            assertNotEquals("", CapSchema.cap_validate_advert(a), String.valueOf(bad));
        }
    }
}
