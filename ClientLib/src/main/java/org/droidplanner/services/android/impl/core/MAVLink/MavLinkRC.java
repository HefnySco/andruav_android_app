package org.droidplanner.services.android.impl.core.MAVLink;

import com.mavlink.common.msg_manual_control;
import com.mavlink.common.msg_rc_channels_override;
import com.o3dr.services.android.lib.model.ICommandListener;

import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;

public class MavLinkRC {
	public static void sendRcOverrideMsg(final MavLinkDrone drone, final int[] rcOutputs) {
		final msg_rc_channels_override msg = new msg_rc_channels_override();
		msg.chan1_raw = (short) rcOutputs[0];
		msg.chan2_raw = (short) rcOutputs[1];
		msg.chan3_raw = (short) rcOutputs[2];
		msg.chan4_raw = (short) rcOutputs[3];
		msg.chan5_raw = (short) rcOutputs[4];
		msg.chan6_raw = (short) rcOutputs[5];
		msg.chan7_raw = (short) rcOutputs[6];
		msg.chan8_raw = (short) rcOutputs[7];
		msg.target_system = drone.getSysid();
		msg.target_component = drone.getCompid();
		drone.getMavClient().sendMessage(msg, null);
	}

	public static void sendManualControl (final MavLinkDrone drone, final int x, final int y, final int z, final int r, final int buttons, ICommandListener listener)
	{
		final msg_manual_control msg = new msg_manual_control();
		msg.target = drone.getSysid();
		msg.compid = drone.getCompid();
		msg.x = (short) x;
		msg.y = (short) y;
		msg.z = (short) z;
		msg.r = (short) r;
		msg.buttons = buttons;

		drone.getMavClient().sendMessage(msg, listener);
	}
}
