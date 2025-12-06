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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;
import org.godotengine.godot.Dictionary;

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
    public static final String[] permissions = {
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
    public void Initialise()
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
    ServiceConnection con;

    boolean serviceStarted = false;
    boolean trackerInitialised = false;

    // Attempts to initialise the step counter as a foreground service
    @UsedByGodot
    public boolean InitialiseService()
    {
        if (serviceStarted) return true;

        if (!HasPermissions()) {
            DisplayToast("Required permissions have not been granted.");
            return false;
        }
        
        Context context = getContext();

        if (stepCounterIntent == null) stepCounterIntent = new Intent (context, StepCounter.class);
        context.startForegroundService(stepCounterIntent);
        serviceStarted = true;

        return SetStepCounterBinding();
    }

    // Stops the service running
    @UsedByGodot
    public void StopService()
    {
        if (stepTracker == null) return;
        DisplayToast("Stoping service");
        stepTracker.UnregisterSensors();
        getContext().unbindService(con);
        getContext().stopService(stepCounterIntent);
        trackerInitialised = false;
        serviceStarted = false;
    }


    // Set binding of stepCounter (to use what it produces)
    // As the service may take up to 5 seconds to start, Godot should call this function
    // periodically until it returns true before attempting to read the step counter
    @UsedByGodot
    public boolean SetStepCounterBinding()
    {
        if (!serviceStarted) return false;

        if (con == null) CreateServiceConnection();

        if (getContext().bindService(stepCounterIntent, con, Context.BIND_AUTO_CREATE)) {
            DisplayToast("Bound service");
            return true;
        } else {
            DisplayToast("Error when binding service");
            return false;
        }
    }


    private void CreateServiceConnection() {
        con = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                StepCounter.LocalBinder binder = (StepCounter.LocalBinder) service;
                stepTracker = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        };
    }


    //region Godot methods

    // Returns sensor data
    @UsedByGodot
    public Dictionary GetStepData()
    {
        if (stepTracker == null || !trackerInitialised)
        {
            DisplayToast("Step tracker is null");
            return null;
        }

        // Convert HashSet to Godot Dictionary
        var dictionary = new Dictionary();
        dictionary.set_keys(new String[]{"steps", "rawsteps"});
        dictionary.set_values(new Object[]{
                stepTracker.stepData.get("steps"),
                stepTracker.stepData.get("rawsteps")
        });
        return dictionary;
    }

    // Resets the step counter
    @UsedByGodot
    public void ResetStepCounter()
    {
        if (stepTracker == null || !trackerInitialised) return;
        stepTracker.ResetCounter();
    }

    // Runs the tracker's init
    @UsedByGodot
    public void InitialiseStepCounter()
    {
        if (stepTracker == null) return;
        if (trackerInitialised) return;

        if(!stepTracker.Initialise())
        {
            DisplayToast("Failed to initialise step counter");
        }
        else
        {
            trackerInitialised = true;
            DisplayToast("Step counter initialised");
        }
    }

    // Register the step counter's sensors
    @UsedByGodot
    public void StartStepCounterSensor()
    {
        if (stepTracker == null || !trackerInitialised) return;
        DisplayToast("Starting sensors");
        stepTracker.RegisterSensors();
    }

    // Manually unregister the sensors
    @UsedByGodot
    public void EndStepCounterSensor()
    {
        if (stepTracker == null || !trackerInitialised) return;
        DisplayToast("Ending sensors");
        stepTracker.UnregisterSensors();
    }

    // Returns if the step counter exists and has its sensors set
    @UsedByGodot
    public boolean IsStepCounterValid()
    {
        if (stepTracker == null) DisplayToast("step counter null on valid");
        if (!trackerInitialised) DisplayToast("step counter not initialised on valid");

        if (stepTracker == null || !trackerInitialised)
        {
            // DisplayToast("Step counter null or isn't initialised");
            return false;
        }

        return stepTracker.IsSensorsAvailable();
    }

    //endregion

}
