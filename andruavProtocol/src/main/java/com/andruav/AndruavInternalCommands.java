package com.andruav;

import android.os.Build;

import com.andruav.protocol.R;
import com.andruav.protocol.commands.Andruav_Parser;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.AndruavMessageBase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mhefny on 5/2/17.
 */

public class AndruavInternalCommands {

    private static final List<AndruavMessageBase> mLocal_2awamer = new ArrayList<AndruavMessageBase>();

    private static boolean mHasExecuted = false;

    public static List<AndruavMessageBase> getList()
    {
        mHasExecuted = true;
        return  mLocal_2awamer;
    }

    public static boolean getHasExecuted ()
    {
        return  mHasExecuted;
    }


    public static void init ()
    {
        loadCommands();
    }


    private static void loadCommands ()
    {
        try {
            mHasExecuted = false;
            mLocal_2awamer.clear();
            InputStream is = AndruavEngine.AppContext.getResources().openRawResource(R.raw.r);

            final int size = is.available();
            if (size == 0) return;
            final byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            {
                json = new String(buffer, StandardCharsets.UTF_8);
            }
            else
            {
                json = new String(buffer);
            }
            final JSONObject json_receive_data = new JSONObject(json);
            final JSONArray jsonArray = json_receive_data.getJSONArray("cmd");
            for (int i=0,l=jsonArray.length(); i<l;++i)
            {
                JSONObject cmd = (JSONObject) jsonArray.get(i);
                final int messageType = Integer.parseInt(cmd.getString(ProtocolHeaders.MessageType));
                final AndruavMessageBase andruavMessageBase = Andruav_Parser.getAndruavMessage(messageType);
                if (cmd.has(ProtocolHeaders.Message)) {
                    andruavMessageBase.setMessageText(cmd.getString(ProtocolHeaders.Message));
                    mLocal_2awamer.add(andruavMessageBase);
                }
            }
            json_receive_data.has("a");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
