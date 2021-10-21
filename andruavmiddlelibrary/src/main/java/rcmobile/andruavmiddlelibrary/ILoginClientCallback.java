package rcmobile.andruavmiddlelibrary;

import com.andruav.event.networkEvent.EventLoginClient;

/**
 * Created by mhefny on 9/24/16.
 */
public interface ILoginClientCallback {
    /**
     * failed to talk to server
     */
    void onError ();

    /***
     * succeeded to talk to server
     */
    void onSuccess (EventLoginClient eventLoginClient);
}
