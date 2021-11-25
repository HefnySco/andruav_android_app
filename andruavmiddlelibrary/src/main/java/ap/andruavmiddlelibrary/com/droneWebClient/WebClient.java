package ap.andruavmiddlelibrary.com.droneWebClient;

import java.util.Map;

/**
 * Created by mhefny on 12/7/16.
 */

public class WebClient extends NanoHTTPD {



    public WebClient() {
        super("0.0.0.0",8080);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();

        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n" + "  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }

        msg += "</body></html>\n";

        return newFixedLengthResponse(msg);
    }
}