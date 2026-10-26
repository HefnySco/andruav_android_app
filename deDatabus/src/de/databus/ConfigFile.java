package de.databus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Java port of de_databus/python/configFile.py — JSON config file with
 * C-style comments (// and /* *\/) stripped before parsing, dotted-key
 * nested merge on update, .bak_<timestamp> backup on save, and the old
 * singleton-style API kept for compatibility.
 */
public class ConfigFile {

    private static ConfigFile _instance;

    private String file_name;
    private JSONObject config = new JSONObject();
    private long _last_write_time = 0;

    public ConfigFile(String file_name) {
        this.file_name = file_name;
        if (file_name != null) {
            this.config = load_config();
            try {
                this._last_write_time = Files.getLastModifiedTime(Paths.get(file_name)).toMillis();
            } catch (IOException ignored) {
            }
        }
    }

    public ConfigFile() {
        this(null);
    }

    /** Strips C-style and C++-style comments so JSON with comments parses. */
    public static String removeComments(String content) {
        content = content.replaceAll("(?s)/\\*.*?\\*/", "");
        content = content.replaceAll("(?m)//.*?$", "");
        return content;
    }

    public JSONObject load_config() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(file_name)), StandardCharsets.UTF_8);
            return new JSONObject(removeComments(content));
        } catch (IOException e) {
            return new JSONObject();
        }
    }

    public Object get_value(String key) {
        return config.get(key);
    }

    public void reload_config() {
        this.config = load_config();
    }

    // --- Backward-compatibility methods (old singleton-style API) ---

    public static synchronized ConfigFile get_instance() {
        if (_instance == null) {
            _instance = new ConfigFile();
        }
        return _instance;
    }

    public void init_config_file(String file_url) {
        this.file_name = file_url;
        this.config = load_config();
        try {
            this._last_write_time = Files.getLastModifiedTime(Paths.get(file_url)).toMillis();
        } catch (IOException ignored) {
        }
    }

    public JSONObject get_config_json() {
        return this.config;
    }

    public void update_json(String json_string) {
        updateJSON(json_string);
    }

    public void reload_file() {
        reload_config();
        try {
            this._last_write_time = Files.getLastModifiedTime(Paths.get(this.file_name)).toMillis();
        } catch (IOException ignored) {
        }
    }

    public boolean file_updated() {
        if (this.file_name == null || !Files.exists(Paths.get(this.file_name))) {
            return false;
        }
        try {
            long mtime = Files.getLastModifiedTime(Paths.get(this.file_name)).toMillis();
            if (mtime == this._last_write_time) {
                return false;
            }
            this._last_write_time = mtime;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Shallow-merges (or nested-merges via dotted keys, e.g.
     * "follow_me.quad.PID_P_X") the given JSON string into the current
     * config and persists it to disk.
     */
    public void updateJSON(String json_string) {
        JSONObject update_json;
        try {
            update_json = new JSONObject(removeComments(json_string));
        } catch (Exception e) {
            System.out.println("Error: Failed to parse update JSON: " + e);
            return;
        }

        for (String key : update_json.keySet()) {
            Object value = update_json.get(key);
            if (key.contains(".")) {
                String[] path_parts = key.split("\\.");
                if (path_parts.length == 0) {
                    System.out.println("Error: Invalid key format: " + key);
                    continue;
                }

                JSONObject current_node = this.config;
                for (int i = 0; i < path_parts.length - 1; i++) {
                    String part = path_parts[i];
                    if (!current_node.has(part) || !(current_node.get(part) instanceof JSONObject)) {
                        current_node.put(part, new JSONObject());
                    }
                    current_node = current_node.getJSONObject(part);
                }

                current_node.put(path_parts[path_parts.length - 1], value);
                System.out.println("Updated/Added nested JSON key: " + key);
            } else {
                this.config.put(key, value);
                System.out.println("Updated/Added JSON key: " + key);
            }
        }

        saveConfigFile();
    }

    public void saveConfigFile() {
        Path p = Paths.get(this.file_name);
        if (Files.exists(p)) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            Path backup = Paths.get(this.file_name + ".bak_" + timestamp);
            try {
                Files.copy(p, backup, StandardCopyOption.COPY_ATTRIBUTES);
                System.out.println("Backup created: " + backup);
            } catch (IOException e) {
                System.out.println("Error: Could not create backup file '" + backup + "': " + e);
            }
        }

        try {
            Files.write(p, this.config.toString(4).getBytes(StandardCharsets.UTF_8));
            System.out.println("Config file saved successfully: " + this.file_name);
        } catch (IOException e) {
            System.out.println("Error: Could not open config file for writing: " + this.file_name + ": " + e);
        }
    }
}
