package org.godotengine.plugin.android.androidtelemetryutils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

    @UsedByGodot
    public void InvokeTestSignal()
    {
        emitSignal("TestSignal", "Hello World!");
    }

    // --- Setup of step manager here ---

    StepCounter stepTracker;
    boolean started = false;

    @UsedByGodot
    public boolean StartStepCounter()
    {
        Context context = getContext();
        Intent intent = new Intent(context, StepCounter.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        {
            context.startForegroundService(intent);
        }
        else
        {
            DisplayToast("Error starting foreground service - API level may be too low?");
            return false;
        }

        // Get binding of stepCounter (to use what it produces)
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
        if(context.bindService(intent, con, Context.BIND_AUTO_CREATE))
        {
            DisplayToast("Bound service");
        }
        else
        {
            DisplayToast("Error when binding service");
            return false;
        }
        started = true;
        return true;
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

}
