package com.creativeshelf.bluetoothsleep;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class ShutoffReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        long end = c.getSharedPreferences("MainActivity", 0).getLong("end", 0);
        if (end == 0 || System.currentTimeMillis() + 2000 < end) return;
        String result;
        try {
            if (Build.VERSION.SDK_INT >= 31 && c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                result = "Bluetooth permission missing";
            } else {
                BluetoothManager manager = (BluetoothManager) c.getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
                if (adapter == null) result = "Bluetooth unavailable";
                else if (!adapter.isEnabled()) result = "Bluetooth already off";
                else {
                    @SuppressWarnings("deprecation") boolean requested = adapter.disable();
                    result = requested ? "Bluetooth shutdown requested" : "Android blocked Bluetooth shutdown";
                }
            }
        } catch (Exception e) { result = "Android blocked Bluetooth shutdown"; }
        c.getSharedPreferences("MainActivity", 0).edit()
            .putLong("end", 0).putString("result", result).apply();
    }
}
