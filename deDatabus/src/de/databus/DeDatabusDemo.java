package de.databus;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Runnable demo: registers a module named 'andruav_demo' with a de_comm
 * on 127.0.0.1. Usage: java -cp <jar> de.databus.DeDatabusDemo [port]
 * (de_comm's s2s port, default 60000).
 */
public class DeDatabusDemo {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 60000;

        CModule module = new CModule();
        module.defineModule(
                CModule.MODULE_CLASS_GENERIC,
                "andruav_demo",
                "andruav_demo_key",
                "1.0.0",
                new JSONArray());
        module.add_module_features(CModule.MODULE_FEATURE_SENDING_TELEMETRY);
        module.setOnReceive((message, len, jMsg) ->
                System.out.println("RX: " + new String(message, 0, Math.min(len, 200))));
        module.init("127.0.0.1", port, "0.0.0.0", 0, 0);

        System.out.println("andruav_demo registered - Ctrl-C to quit");
        Thread.sleep(Long.MAX_VALUE);
    }
}
