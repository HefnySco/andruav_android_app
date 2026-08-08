package com.andruav.event.fpv7adath;

/**
 * Posted whenever FPVStreamingService starts or stops, so app-wide UI (e.g. the FPV button on
 * the home screen) can refresh. Carries no payload - subscribers should re-query current state
 * via App.isFPVStreamingServiceRunning(), same pattern as Event_ProtocolChanged/Event_GCSBlockedChanged.
 */
public class _7adath_FPVStreamingStatusChanged {
}
