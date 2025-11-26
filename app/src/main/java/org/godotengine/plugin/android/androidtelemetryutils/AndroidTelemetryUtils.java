package org.godotengine.plugin.android.androidtelemetryutils;

import android.widget.Toast;

import androidx.annotation.NonNull;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

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
}
