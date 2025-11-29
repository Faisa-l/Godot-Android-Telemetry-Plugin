package org.godotengine.plugin.android.androidtelemetryutils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AndroidTelemetryUtils extends GodotPlugin {
    /**
     * Base constructor passing a {@link Godot} instance through which the plugin can access Godot's
     * APIs and lifecycle events.
     *
     * @param godot
     */
    public AndroidTelemetryUtils(Godot godot) {
        super(godot);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return BuildConfig.GODOT_PLUGIN_NAME;
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals()
    {
        Set<SignalInfo> signals = new HashSet<>();

        /* To add signal:
            - Create new SignalInfo class
            - First argument is the name of the signal
            - Next arguments are the return types for the signal
            - Effectively the same as 'signal [NAME](T, T...)' in gdscript

           Connect this signal to a method in Godot. This plugin will invoke the signal.
         */

        signals.add(new SignalInfo("TestSignal", String.class));

        return signals;
    }

    @UsedByGodot
    public void DisplayToast(String message)
    {
        getGodot().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getGodot().getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Request the activity recognition permission
    // Returns whether this was already accepted
    static final String[] permissions = {
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE};
    @UsedByGodot
    public void RequestPermissions()
    {
        if (HasPermissions()) return;

        boolean shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACTIVITY_RECOGNITION);
        if (shouldShow)
        {
            // - The correct thing to do would be to prompt a AlertDialog box here
            // - However since this solely exists for demonstrative purposes for how this system
            //   should work, it would be unnecessary to do
            // - Also doesn't help that that code looks very ugly
        }
        // Requests permissions; code will need to check whether this was accepted or not
        getActivity().requestPermissions(permissions, 1);
    }

    @UsedByGodot
    private boolean HasPermissions() {
        for (String permission : permissions)
        {
            if (getContext().checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    @UsedByGodot
    public void InvokeTestSignal()
    {
        emitSignal("TestSignal", "Hello World!");
    }

    @UsedByGodot
    public void Initalise()
    {
        CreateNotificationChannel();

    }

    // --- Notification channel setup ---
    NotificationChannel channel;
    NotificationManager notificationManager;
    public static final String CHANNEL_ID = "ACTIVITY_TRACKING";
    private void CreateNotificationChannel()
    {
        notificationManager = getContext().getSystemService(NotificationManager.class);
        channel = new NotificationChannel(
                CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        notificationManager.createNotificationChannel(channel);
    }

    // --- Setup of step manager here ---

    StepCounter stepTracker;
    Intent stepCounterIntent;
    boolean started = false;

    // Attempts to initialise the step counter as a foreground service
    @UsedByGodot
    public boolean StartStepCounter()
    {
        if (!HasPermissions()) {
            DisplayToast("Required permissions have not been granted.");
            return false;
        }
        
        Context context = getContext();

        if (stepCounterIntent == null) stepCounterIntent = new Intent (context, StepCounter.class);
        context.startForegroundService(stepCounterIntent);
        started = true;

        if (SetStepCounterBinding()) return false;
        return true;
    }


    // Set binding of stepCounter (to use what it produces)
    // As the service may take up to 5 seconds to start, Godot should call this function
    // periodically until it returns true before attempting to read the step counter
    @UsedByGodot
    public boolean SetStepCounterBinding()
    {
        if (!started) return false;

        ServiceConnection con = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                StepCounter.LocalBinder binder = (StepCounter.LocalBinder) service;
                stepTracker = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };

        if(getContext().bindService(stepCounterIntent, con, Context.BIND_AUTO_CREATE))
        {
            DisplayToast("Bound service");
            return true;
        }
        else
        {
            DisplayToast("Error when binding service");
            return false;
        }
    }

    @UsedByGodot
    public HashMap<String, Float> GetStepData()
    {
        return (stepTracker != null) ? stepTracker.stepData : null;
    }

    @UsedByGodot
    public void ResetStepCounter()
    {
        if (stepTracker == null) return;
        stepTracker.ResetCounter();
    }

    @UsedByGodot
    public void UnregisterSensors()
    {
        if (stepTracker == null) return;
        stepTracker.UnregisterSensors();
    }

    @UsedByGodot
    public boolean IsStepCounterValid()
    {
        return stepTracker != null && stepTracker.IsSensorsAvailable();
    }


}
