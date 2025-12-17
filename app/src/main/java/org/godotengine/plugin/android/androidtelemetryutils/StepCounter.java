package org.godotengine.plugin.android.androidtelemetryutils;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.core.app.ServiceCompat;

import java.util.HashMap;

public class StepCounter extends Service implements SensorEventListener {
    SensorManager sensorManager;
    Sensor stepCounter;
    float startingSteps;
    public HashMap<String, Float> stepData;

    private boolean registered;
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        StepCounter getService() {
            return StepCounter.this;
        }
    }


    public boolean Initialise()
    {
        registered = false;
        if (SetDeviceSensors())
        {
            stepData = new HashMap<String, Float>();
            ResetCounter();
            return true;
        }
        return false;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        // Initialise();
    }

    @Override
    public void onDestroy()
    {
        UnregisterSensors();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // String[] permissions = {Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS};
        for (String permission : AndroidTelemetryUtils.permissions)
        {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED)
            {
                stopSelf();
                return START_NOT_STICKY;
            }
        }


        // Make the notification
        Notification notification = new Notification.Builder(this, AndroidTelemetryUtils.CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Step Counter")
                .setContentText("Tracking steps")
                .build();

        ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);

        return START_STICKY;
    }

    public void ResetCounter()
    {
        // Setting startingSteps to 0 will reset its value to the current step
        startingSteps = 0f;
        stepData.put("steps", 0f);
        stepData.put("rawsteps", 0f);
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
        stepData.put("steps", sensorEvent.values[0] - startingSteps);
        stepData.put("rawsteps", sensorEvent.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Returns true if sensor manager and step counter are available
    public boolean IsSensorsAvailable()
    {
        return sensorManager != null && stepCounter != null;
    }

    public Boolean SetDeviceSensors()
    {
        // Early exit in case this was already set
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
        }

        return IsSensorsAvailable();
    }

    public void RegisterSensors()
    {
        if (!IsSensorsAvailable()) return;
        if (registered) return;

        if (!IsSensorsAvailable() && registered) return;
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        registered = true;
    }

    public void UnregisterSensors()
    {
        if (!IsSensorsAvailable()) return;
        if (!registered) return;

        sensorManager.unregisterListener(this);
        registered = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


}
