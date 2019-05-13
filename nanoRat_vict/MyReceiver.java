package com.example.nyadav.service;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


public class MyReceiver extends BroadcastReceiver {
    public static Context mContext;
    Context context;

    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, MyService.class));


        }
        mContext = context;

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_POWER_CONNECTED)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }
        }

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_POWER_DISCONNECTED)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));
            }
        }


        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_DATE_CHANGED)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }
        }


        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }
        }


        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }
        }


        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_ON)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }

        }


        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_USER_UNLOCKED)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }

        }


        if (!intent.getAction().equalsIgnoreCase(Intent.ACTION_UID_REMOVED)) {
            if (!isMyServiceRunning()) {
                context.startService(new Intent(context, MyService.class));

            }
            String bro = intent.getAction();

        }





    }












    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



}
