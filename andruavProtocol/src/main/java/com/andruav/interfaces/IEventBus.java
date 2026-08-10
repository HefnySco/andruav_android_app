package com.andruav.interfaces;



/**
 * Created by mhefny on 2/21/16.
 */
public interface IEventBus {

    void post (Object object);

    /**
     * Like {@link #post(Object)}, but caches the event so a subscriber that registers with
     * a sticky handler after this call still receives it immediately - closes the race where
     * a signal (e.g. WebRTC "joinme") arrives before its listener has finished initializing.
     */
    void postSticky (Object object);
}
