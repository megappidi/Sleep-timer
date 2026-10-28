package com.creativeshelf.bluetoothsleep;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView countdown, info;
    private int pendingMinutes = 60;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 1000); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(42, 42, 42, 42);
        layout.setBackgroundColor(0xFF141727);
        setContentView(layout);
        TextView title = label("☾ Bluetooth Sleep Timer", 24, 0xFFF6F5FF);
        layout.addView(title);
        TextView subtitle = label("Fall asleep. We'll disconnect your earphones.", 14, 0xFFAFBADD);
        layout.addView(subtitle);
        countdown = label("60:00", 54, 0xFFC8CFFF);
        countdown.setPadding(0, 35, 0, 30);
        layout.addView(countdown);
        info = label("Choose a timer to begin", 14, 0xFFAFBADD);
        layout.addView(info);
        Button hour = button("Start 60-minute timer", 0xFFBBC5FF);
        layout.addView(hour);
        hour.setOnClickListener(v -> startTimer(60));
        Button test = button("Test for 1 minute", 0xFF9DA8D6);
        layout.addView(test);
        test.setOnClickListener(v -> startTimer(1));
        Button cancel = button("Cancel timer", 0xFF4B526D);
        cancel.setTextColor(0xFFFFFFFF);
        layout.addView(cancel);
        cancel.setOnClickListener(v -> cancelTimer());
    }
    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color); t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }
    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(color));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 11, 0, 0); b.setLayoutParams(p);
        return b;
    }
    private PendingIntent alarm() {
        Intent i = new Intent(this, ShutoffReceiver.class);
        return PendingIntent.getBroadcast(this, 100, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private void startTimer(int minutes) {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            pendingMinutes = minutes;
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 7);
            return;
        }
        long end = System.currentTimeMillis() + minutes * 60_000L;
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent open = PendingIntent.getActivity(this, 101,
            new Intent(this, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(alarm());
        // Alarm-clock scheduling is permitted without a special exact-alarm permission.
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(end, open), alarm());
        getPreferences(0).edit().putLong("end", end).putString("result", "").apply();
        refresh();
    }
    private void cancelTimer() {
        ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(alarm());
        getPreferences(0).edit().putLong("end", 0).putString("result", "Timer cancelled").apply();
        refresh();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] perms, int[] grants) {
        super.onRequestPermissionsResult(requestCode, perms, grants);
        if (requestCode == 7 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED)
            startTimer(pendingMinutes);
        else if (requestCode == 7) info.setText("Nearby devices permission required");
    }
    private void refresh() {
        long remaining = getPreferences(0).getLong("end", 0) - System.currentTimeMillis();
        if (getPreferences(0).getLong("end", 0) > 0 && remaining > 0) {
            long seconds = (remaining + 999) / 1000;
            countdown.setText(String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
            info.setText("Bluetooth switches off when the timer finishes");
        } else {
            countdown.setText("☾");
            String result = getPreferences(0).getString("result", "");
            info.setText(result.isEmpty() ? "Choose a timer to begin" : result);
        }
    }
    @Override public void onStart() { super.onStart(); handler.post(tick); }
    @Override public void onStop() { handler.removeCallbacks(tick); super.onStop(); }
}
