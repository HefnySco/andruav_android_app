package de.databus;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Java port of de_databus/python/cap_schema.py — the de.cap/1 advert
 * dialect: validator subset + advert hash, mirroring
 * de_common/de_databus/cap_schema.cpp.
 *
 * JSON values arrive as org.json types; the helpers below reproduce
 * Python's isinstance() semantics (bool is not int, JSONObject is dict,
 * JSONArray is list, Integer/Long/BigDecimal-scale-0 is int, Double is
 * number-not-int).
 */
public final class CapSchema {

    private CapSchema() {}

    public static final String CAP_SCHEMA_ID = "de.cap/1";
    public static final int CAP_ADVERT_MAX_BYTES = 16 * 1024;
    public static final int CAP_NAME_MAX_LEN = 32;

    private static final Set<String> CAP_SCALAR_TYPES = new HashSet<>(Arrays.asList(
            "bool", "int", "number", "string", "enum", "geo", "array", "object"));

    private static final java.util.regex.Pattern CAP_NAME_RE =
            java.util.regex.Pattern.compile("^[a-z][a-z0-9_]*$");

    // ----- Python-isinstance equivalents for org.json values -----

    private static boolean isDict(Object v)  { return v instanceof JSONObject; }
    private static boolean isList(Object v)  { return v instanceof JSONArray; }
    private static boolean isStr(Object v)   { return v instanceof String; }
    private static boolean isBool(Object v)  { return v instanceof Boolean; }

    private static boolean isInt(Object v) {
        if (v instanceof Boolean) return false;
        if (v instanceof Integer || v instanceof Long) return true;
        if (v instanceof BigDecimal) return ((BigDecimal) v).stripTrailingZeros().scale() <= 0;
        return false;
    }

    private static boolean isNum(Object v) {
        return v instanceof Number && !(v instanceof Boolean);
    }

    /** ch = first 16 hex chars of SHA-256 over the advert strings exactly
     *  as sent, joined with '\n' in the order sent. */
    public static String cap_advert_hash(List<String> adverts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.join("\n", adverts).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean cap_is_valid_name(Object name) {
        return name instanceof String && ((String) name).length() <= CAP_NAME_MAX_LEN
                && CAP_NAME_RE.matcher((String) name).matches();
    }

    /** 'type' may be a single type name or a list of scalar type names. */
    private static List<String> _type_list(JSONObject param) {
        Object t = param.opt("type");
        if (t instanceof String) return Arrays.asList((String) t);
        if (t instanceof JSONArray) {
            JSONArray arr = (JSONArray) t;
            if (arr.length() == 0) return null;
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                if (!(arr.get(i) instanceof String)) return null;
                out.add(arr.getString(i));
            }
            return out;
        }
        return null;
    }

    private static boolean _value_matches(Object value, String type_name) {
        switch (type_name) {
            case "bool":   return isBool(value);
            case "int":    return isInt(value);
            case "number": return isNum(value);
            case "string": return isStr(value);
            case "geo":
                return isList(value) && ((JSONArray) value).length() == 3
                        && isNum(((JSONArray) value).get(0))
                        && isNum(((JSONArray) value).get(1))
                        && isNum(((JSONArray) value).get(2));
            case "enum":   return isStr(value) || isNum(value) || isBool(value);
            case "array":  return isList(value);
            case "object": return isDict(value);
            default:       return false;
        }
    }

    private static void _check_value(JSONObject param, Object value, String path,
                                     List<String> errors, int depth) {
        List<String> types = _type_list(param);
        if (types == null) {
            errors.add(path + ": schema has no usable 'type'");
            return;
        }
        String t = null;
        for (String c : types) {
            if (_value_matches(value, c)) { t = c; break; }
        }
        if (t == null) {
            errors.add(path + ": expected " + String.join("|", types));
            return;
        }

        if (t.equals("int") || t.equals("number")) {
            double v = ((Number) value).doubleValue();
            if (param.has("min") && v < param.getDouble("min"))
                errors.add(path + ": must be >= " + param.get("min"));
            if (param.has("max") && v > param.getDouble("max"))
                errors.add(path + ": must be <= " + param.get("max"));
        }
        if (t.equals("enum")) {
            JSONArray values = param.optJSONArray("values");
            boolean found = false;
            if (values != null) {
                for (int i = 0; i < values.length(); i++) {
                    if (values.get(i).equals(value)) { found = true; break; }
                }
            }
            if (!found) errors.add(path + ": not one of the allowed values");
        }
        if (t.equals("array") && param.has("items") && depth < 4) {
            JSONArray arr = (JSONArray) value;
            for (int i = 0; i < arr.length(); i++) {
                _check_value(param.getJSONObject("items"), arr.get(i),
                        path + "[" + i + "]", errors, depth + 1);
            }
        }
        if (t.equals("object") && param.has("fields") && depth < 4) {
            JSONObject fields = param.getJSONObject("fields");
            JSONObject obj = (JSONObject) value;
            for (String fname : fields.keySet()) {
                JSONObject fparam = fields.getJSONObject(fname);
                if (obj.has(fname)) {
                    _check_value(fparam, obj.get(fname), path + "." + fname, errors, depth + 1);
                } else if (!fparam.optBoolean("optional") && !fparam.has("default")) {
                    errors.add(path + "." + fname + ": missing required field");
                }
            }
        }
    }

    /** Check invoke params against an action's "params" schema.
     *  Returns a list of error strings; `filled` gets `with_` + defaults. */
    public static List<String> cap_check_params(Object params_schema, Object with_,
                                                Map<String, Object> filled) {
        List<String> errors = new ArrayList<>();
        filled.clear();
        if (with_ instanceof JSONObject) {
            JSONObject w = (JSONObject) with_;
            for (String k : w.keySet()) filled.put(k, w.get(k));
        }

        if (params_schema == null) return errors;
        if (!(params_schema instanceof JSONObject)) {
            errors.add("params schema is not an object");
            return errors;
        }
        if (with_ != null && !(with_ instanceof JSONObject)) {
            errors.add("with: expected an object");
        }

        JSONObject schema = (JSONObject) params_schema;
        if (with_ instanceof JSONObject) {
            JSONObject w = (JSONObject) with_;
            for (String key : w.keySet()) {
                if (!schema.has(key)) errors.add(key + ": unknown param");
            }
        }

        for (String name : schema.keySet()) {
            JSONObject param = schema.getJSONObject(name);
            if (with_ instanceof JSONObject && ((JSONObject) with_).has(name)) {
                _check_value(param, ((JSONObject) with_).get(name), name, errors, 0);
            } else if (param.has("default")) {
                filled.put(name, param.get("default"));
            } else if (!param.optBoolean("optional")) {
                errors.add(name + ": missing required param");
            }
        }

        return errors;
    }

    private static String _validate_param(Object paramObj, String path, int depth) {
        if (depth > 4) return path + ": param nesting too deep";
        if (!(paramObj instanceof JSONObject)) return path + ": param must be an object";
        JSONObject param = (JSONObject) paramObj;
        List<String> types = _type_list(param);
        if (types == null) return path + ": missing or invalid 'type'";
        for (String t : types) {
            if (!CAP_SCALAR_TYPES.contains(t)) return path + ": unknown type '" + t + "'";
        }
        if (types.size() > 1) {
            for (String t : types) {
                if (t.equals("enum") || t.equals("geo") || t.equals("array") || t.equals("object"))
                    return path + ": type list allows scalar types only";
            }
        }
        String t = types.get(0);
        if (t.equals("enum") && param.optJSONArray("values") == null)
            return path + ": enum requires a non-empty 'values' array";
        if (t.equals("array") && param.has("items")) {
            String r = _validate_param(param.opt("items"), path + "[]", depth + 1);
            if (r != null) return r;
        }
        if (t.equals("object") && param.has("fields")) {
            if (!(param.opt("fields") instanceof JSONObject))
                return path + ": 'fields' must be an object";
            JSONObject fields = param.getJSONObject("fields");
            for (String fname : fields.keySet()) {
                if (!cap_is_valid_name(fname)) return path + ": bad field name '" + fname + "'";
                String r = _validate_param(fields.get(fname), path + "." + fname, depth + 1);
                if (r != null) return r;
            }
        }
        if (param.has("min") && !isNum(param.get("min"))) return path + ": 'min' must be a number";
        if (param.has("max") && !isNum(param.get("max"))) return path + ": 'max' must be a number";
        if (param.has("min") && param.has("max")
                && isNum(param.get("min")) && isNum(param.get("max"))
                && ((Number) param.get("min")).doubleValue() > ((Number) param.get("max")).doubleValue())
            return path + ": 'min' > 'max'";
        if (param.has("default")) {
            JSONObject singleSchema = new JSONObject().put(path, param);
            JSONObject singleWith = new JSONObject().put(path, param.get("default"));
            List<String> errs = cap_check_params(singleSchema, singleWith, new java.util.HashMap<>());
            if (!errs.isEmpty()) return path + ": bad default (" + errs.get(0) + ")";
        }
        return null;
    }

    private static String _validate_param_map(Object mapObj, String path) {
        if (!(mapObj instanceof JSONObject)) return path + " must be an object";
        JSONObject map = (JSONObject) mapObj;
        for (String name : map.keySet()) {
            if (!cap_is_valid_name(name)) return path + ": bad name '" + name + "'";
            String r = _validate_param(map.get(name), path + "." + name, 0);
            if (r != null) return r;
        }
        return null;
    }

    /** Returns "" on success or an error string. */
    public static String cap_validate_advert(Object advertObj) {
        if (!(advertObj instanceof JSONObject)) return "advert must be an object";
        JSONObject advert = (JSONObject) advertObj;
        if (!CAP_SCHEMA_ID.equals(advert.optString("schema")))
            return "missing or wrong 'schema' (expected " + CAP_SCHEMA_ID + ")";
        if (!cap_is_valid_name(advert.opt("ns"))) return "missing or bad 'ns'";
        if (!(advert.opt("module") instanceof String)) return "missing 'module'";
        if (!(advert.opt("ver") instanceof String)) return "missing 'ver'";

        Object actionsObj = advert.opt("actions");
        if (actionsObj != null) {
            if (!(actionsObj instanceof JSONObject)) return "'actions' must be an object";
            JSONObject actions = (JSONObject) actionsObj;
            for (String name : actions.keySet()) {
                String path = "actions." + name;
                if (!cap_is_valid_name(name)) return path + ": bad action name";
                if (!(actions.get(name) instanceof JSONObject)) return path + " must be an object";
                JSONObject act = actions.getJSONObject(name);
                if (act.has("desc") && !(act.get("desc") instanceof String))
                    return path + ": 'desc' must be a string";
                if (act.has("timeout_s") && !(isNum(act.get("timeout_s"))
                        && ((Number) act.get("timeout_s")).doubleValue() > 0))
                    return path + ": 'timeout_s' must be a positive number";
                if (act.has("deprecated_alias") && !(act.get("deprecated_alias") instanceof String))
                    return path + ": 'deprecated_alias' must be a string";
                // TASK-PF-01: "claim" narrows a sensor-lane claim from the
                // whole namespace to this action only ("action"); absent or
                // "ns" keeps the namespace-wide behaviour
                if (act.has("claim") && !("ns".equals(act.get("claim")) || "action".equals(act.get("claim"))))
                    return path + ": 'claim' must be \"ns\" or \"action\"";
                if (act.has("params")) {
                    String r = _validate_param_map(act.get("params"), path + ".params");
                    if (r != null) return r;
                }
            }
        }

        Object eventsObj = advert.opt("events");
        if (eventsObj != null) {
            if (!(eventsObj instanceof JSONObject)) return "'events' must be an object";
            JSONObject events = (JSONObject) eventsObj;
            for (String name : events.keySet()) {
                String path = "events." + name;
                if (!cap_is_valid_name(name)) return path + ": bad event name";
                if (!(events.get(name) instanceof JSONObject)) return path + " must be an object";
                JSONObject ev = events.getJSONObject(name);
                if (ev.has("payload")) {
                    String r = _validate_param_map(ev.get("payload"), path + ".payload");
                    if (r != null) return r;
                }
            }
        }

        if (advert.has("state")) {
            String r = _validate_param_map(advert.get("state"), "state");
            if (r != null) return r;
        }

        // TASK-PF-02: the event surfaces of dynamic per-resource names
        // "<ns>.<key>.<verb>". Both keys are additive: pre-PF-02 validators
        // ignore unknown top-level keys, so a PF-02 advert still validates
        // (and is forwarded verbatim) by an old de_comm.

        Object resourcesObj = advert.opt("resources");
        if (resourcesObj != null) {
            if (!(resourcesObj instanceof JSONObject)) return "'resources' must be an object";
            JSONObject resources = (JSONObject) resourcesObj;
            for (String rname : resources.keySet()) {
                if (!cap_is_valid_name(rname)) return "resources: bad resource name '" + rname + "'";
                Object cls = resources.get(rname);
                if (!(cls instanceof String) || !cap_is_valid_name(cls))
                    return "resources." + rname + ": bad class name";
            }
        }

        Object patternsObj = advert.opt("event_patterns");
        if (patternsObj != null) {
            if (!(patternsObj instanceof JSONObject)) return "'event_patterns' must be an object";
            JSONObject patterns = (JSONObject) patternsObj;
            for (String key : patterns.keySet()) {
                String path = "event_patterns." + key;
                // keys are exactly "*.<verb>" with <verb> a cap name
                if (key.length() < 3 || key.charAt(0) != '*' || key.charAt(1) != '.'
                        || !cap_is_valid_name(key.substring(2)))
                    return path + ": pattern keys must be '*.<verb>'";
                if (!(patterns.get(key) instanceof JSONObject)) return path + " must be an object";
                JSONObject pat = patterns.getJSONObject(key);
                if (pat.has("kind") && !("fact".equals(pat.get("kind")) || "command".equals(pat.get("kind"))))
                    return path + ": 'kind' must be 'fact' or 'command'";
                if (pat.has("res")) {
                    Object res = pat.get("res");
                    if (!(res instanceof JSONArray)) return path + ": 'res' must be an array";
                    JSONArray resArr = (JSONArray) res;
                    for (int i = 0; i < resArr.length(); i++) {
                        if (!(resArr.get(i) instanceof String) || !cap_is_valid_name(resArr.get(i)))
                            return path + ": 'res' entries must be class names";
                    }
                }
                if (pat.has("desc") && !(pat.get("desc") instanceof String))
                    return path + ": 'desc' must be a string";
                if (pat.has("payload")) {
                    String r = _validate_param_map(pat.get("payload"), path + ".payload");
                    if (r != null) return r;
                }
            }
        }

        return "";
    }

    /** Match a dynamic per-resource name against the advert's
     *  "event_patterns" + "resources" (TASK-PF-02). Returns the pattern
     *  JSONObject, or null. Pure: the caller validates the advert first. */
    public static JSONObject cap_match_event_pattern(JSONObject advert, String key,
                                                     String verb, String kind) {
        if (advert == null) return null;
        Object patternsObj = advert.opt("event_patterns");
        if (!(patternsObj instanceof JSONObject)) return null;
        Object patObj = ((JSONObject) patternsObj).opt("*." + verb);
        if (!(patObj instanceof JSONObject)) return null;
        JSONObject pat = (JSONObject) patObj;
        if (!kind.equals(pat.optString("kind", "fact"))) return null;

        // the resource check applies only when the advert lists resources:
        // without it the key cannot be class-checked and the pattern wins
        Object resourcesObj = advert.opt("resources");
        if (resourcesObj != null) {
            if (!(resourcesObj instanceof JSONObject)) return null;
            Object cls = ((JSONObject) resourcesObj).opt(key);
            if (!(cls instanceof String)) return null;
            Object res = pat.opt("res");
            if (res instanceof JSONArray) {
                JSONArray resArr = (JSONArray) res;
                boolean found = false;
                for (int i = 0; i < resArr.length(); i++) {
                    if (cls.equals(resArr.get(i))) { found = true; break; }
                }
                if (!found) return null;
            }
        }

        return pat;
    }

    /** action `act` of an advert, by name or by its "deprecated_alias". */
    public static JSONObject cap_find_action(JSONObject advert, String act) {
        JSONObject actions = advert != null ? advert.optJSONObject("actions") : null;
        if (actions == null) return null;
        if (actions.has(act)) return actions.getJSONObject(act);
        for (String name : actions.keySet()) {
            Object a = actions.get(name);
            if (a instanceof JSONObject && act.equals(((JSONObject) a).optString("deprecated_alias")))
                return (JSONObject) a;
        }
        return null;
    }

    /** current name of action `act` (itself or the action it is an alias of). */
    public static String cap_canonical_action(JSONObject advert, String act) {
        JSONObject actions = advert != null ? advert.optJSONObject("actions") : null;
        if (actions == null) return null;
        if (actions.has(act)) return act;
        for (String name : actions.keySet()) {
            Object a = actions.get(name);
            if (a instanceof JSONObject && act.equals(((JSONObject) a).optString("deprecated_alias")))
                return name;
        }
        return null;
    }

    /** Canonical compact JSON with sorted keys (matches Python's
     *  json.dumps(..., separators=(',',':'), sort_keys=True)). */
    public static String canonicalJson(Object value) {
        if (value == null || value == JSONObject.NULL) return "null";
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (String k : obj.keySet()) sorted.put(k, obj.get(k));
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : sorted.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(JSONObject.quote(e.getKey())).append(':').append(canonicalJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) sb.append(',');
                sb.append(canonicalJson(arr.get(i)));
            }
            return sb.append(']').toString();
        }
        if (value instanceof String) return JSONObject.quote((String) value);
        if (value instanceof Number) {
            Number n = (Number) value;
            double d = n.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
            return n.toString();
        }
        return value.toString();
    }

    /** Accept advert strings or JSONObjects; returns [strings, parsed].
     *  JSONObjects are serialized canonically (sorted keys, compact) and
     *  that serialization is what is hashed and sent. */
    public static List<Object> cap_normalize_adverts(List<Object> adverts) {
        List<String> strings = new ArrayList<>();
        List<JSONObject> parsed = new ArrayList<>();
        for (Object a : adverts) {
            String s = (a instanceof String) ? (String) a : canonicalJson(a);
            strings.add(s);
            parsed.add(new JSONObject(s));
        }
        List<Object> out = new ArrayList<>();
        out.add(strings);
        out.add(parsed);
        return out;
    }
}
