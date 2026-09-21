package org.droidplanner.services.android.impl.communication.connection;

import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

/**
 * Created by fredia on 3/28/16.
 */
public abstract class AndroidIpConnection extends AndroidMavLinkConnection {

    protected AndroidIpConnection(Context context){
        super(context);
    }

    @Override
    protected final void openConnection(Bundle connectionExtras) throws IOException {
        onOpenConnection(connectionExtras);
    }

    protected abstract void onOpenConnection(Bundle extras) throws IOException;

    @Override
    protected final void closeConnection() throws IOException {
        onCloseConnection();
    }

    protected abstract void onCloseConnection() throws IOException;

}
