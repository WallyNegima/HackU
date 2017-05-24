package com.example.wally_nagama.paripigrass;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by wally_nagama on 2017/05/24.
 */

public class BlueToothReceiver extends BroadcastReceiver {
    public Activity mActivity;
    private String devList;
    private TextView btText;

    public BlueToothReceiver(){

    }

    public BlueToothReceiver(Activity activity, String str, TextView textview){
        mActivity = activity;
        devList = str;
        this.btText = textview;
    }

    public void onReceive(Context context, Intent intent){
        String action = intent.getAction();
        Log.d("bt", "何か起きた");
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // Add the name and address to an array adapter to show in a ListView
            devList += (device.getName() + "\n" + device.getAddress());
            //devList += (device.getAddress());
            btText.setText(devList);
        }
    }

    public void register() {
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mActivity.registerReceiver(this, bluetoothFilter);
    }
    public void unregister(){
        mActivity.unregisterReceiver(this);
    }

}
