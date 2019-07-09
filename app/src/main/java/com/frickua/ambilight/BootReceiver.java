package com.frickua.ambilight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG_BOOT_BROADCAST_RECEIVER = "BOOT_BROADCAST_RECEIVER";


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        String message = "BootDeviceReceiver onReceive, action is " + action;

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        Log.d(TAG_BOOT_BROADCAST_RECEIVER, action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
//            AmbilightService.startActionBaz(context, null, null);
        }
    }
}
