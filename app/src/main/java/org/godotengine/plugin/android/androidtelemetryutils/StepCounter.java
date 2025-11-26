package org.godotengine.plugin.android.androidtelemetryutils;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.ServiceCompat;

import java.util.HashMap;

/** TODO:
 * - Somehow ask for permission somewhere
 * - This class's foreground tasks would be started and stopped by the Godot plugin class, which
 *   would come from Godot itself.
 *
 */

public class StepCounter extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor stepCounter;
    float startingSteps;

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        StepCounter getService() {
            // Return this instance of LocalService so clients can call public methods.
            return StepCounter.this;
        }
    }

    public HashMap<String, Float> stepData;

    public boolean Initialise()
    {
        if (SetDeviceSensors())
        {
            stepData = new HashMap<>();
            stepData.put("stepCounter", 0f);
            startingSteps = 0f;

            RegisterSensors();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Initialise();
    }

    @Override
    public void onDestroy()
    {
        UnregisterSensors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Intent counterIntent = new Intent(this, StepCounter.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                counterIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Make the notification
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Step Counter")
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        }

        return START_STICKY;
    }

    public void ResetCounter()
    {
        // Setting startingSteps to 0 will reset its value to the current step
        startingSteps = 0f;
        stepData.put("stepCounter", 0f);
    }

    // This is where we actually see if the event has done anything
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent == null)
        {
            return;
        }
        if (startingSteps == 0f)
        {
            startingSteps = sensorEvent.values[0];
        }
        stepData.put("stepCounter", sensorEvent.values[0] - startingSteps);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public boolean IsSensorsAvailable()
    {
        return (sensorManager != null && stepCounter != null);
    }

    public Boolean SetDeviceSensors()
    {
        if (IsSensorsAvailable()) return true;

        // Get sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null)
        {
            return false;
        }

        // Get step counter
        if (stepCounter == null)
        {
            stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            return stepCounter != null;
        }
        return true;
    }

    public void RegisterSensors()
    {
        if (!IsSensorsAvailable()) return;
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_GAME);
    }

    public void UnregisterSensors()
    {
        if (!IsSensorsAvailable()) return;
        sensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


}
