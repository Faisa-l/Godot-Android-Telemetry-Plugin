package org.godotengine.plugin.android.androidtelemetryutils;

import android.widget.Toast;

import androidx.annotation.NonNull;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.UsedByGodot;

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

    @UsedByGodot
    public void DisplayToast()
    {
        getGodot().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getGodot().getActivity(), "Hello World", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
